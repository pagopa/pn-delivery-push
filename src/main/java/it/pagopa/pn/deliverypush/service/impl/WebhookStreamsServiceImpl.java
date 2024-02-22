package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WebhookStreamsServiceImpl extends WebhookServiceImpl implements WebhookStreamsService {

    public static final String ERROR_CREATING_STREAM = "error creating stream";
    private final SchedulerService schedulerService;
    private final PnExternalRegistryClient pnExternalRegistryClient;

    private int purgeDeletionWaittime;

    public WebhookStreamsServiceImpl (StreamEntityDao streamEntityDao, SchedulerService schedulerService
        , PnDeliveryPushConfigs pnDeliveryPushConfigs, PnExternalRegistryClient pnExternalRegistryClient){
        super(streamEntityDao, pnDeliveryPushConfigs);
        this.schedulerService = schedulerService;
        this.pnExternalRegistryClient = pnExternalRegistryClient;
    }

    @PostConstruct
    private void postConstruct() {
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.purgeDeletionWaittime = webhookConf.getPurgeDeletionWaittime();
    }
    @Override
    public Mono<StreamMetadataResponseV23> createEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV23> streamCreationRequest) {
        final String apiV10 = pnDeliveryPushConfigs.getWebhook().getFirstVersion();
        String msg = "createEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}";
        String[] args = {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion};

        return streamCreationRequest.doOnNext(payload-> {
                String[] fullArgs = ArrayUtils.add(args, payload.toString());
                generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg+", request={} ", fullArgs).log();
            }).flatMap(x->
                (x.getReplacedStreamId() == null ? checkStreamCount(xPagopaPnCxId) : Mono.just(Boolean.TRUE)).then(Mono.just(x))
            )
            .flatMap(dto -> {
                List<String> allowedGroups = CollectionUtils.isEmpty(xPagopaPnCxGroups)
                    ? pnExternalRegistryClient.getGroups(xPagopaPnUid, xPagopaPnCxId)
                    : xPagopaPnCxGroups;

                if (CollectionUtils.isEmpty(dto.getGroups()) && !apiV10.equals(xPagopaPnApiVersion) && !CollectionUtils.isEmpty(xPagopaPnCxGroups)){
                    return Mono.error(new PnWebhookForbiddenException("Not Allowed empty groups for apikey with groups "+groupString(xPagopaPnCxGroups) +" when Api Version is "+xPagopaPnApiVersion));
                } else {
                    return WebhookUtils.checkGroups(dto.getGroups(), allowedGroups) ?
                        saveOrReplace(dto, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion)
                        : Mono.error(new PnWebhookForbiddenException("Not Allowed groups " + groupString(dto.getGroups())));
                }
            }).map(EntityToDtoStreamMapper::entityToDto).doOnSuccess(newEntity-> generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg, args).generateSuccess().log()).doOnError(err-> generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg, args).generateFailure(ERROR_CREATING_STREAM, err).log());
    }

    private Mono<StreamEntity> saveOrReplace(StreamCreationRequestV23 dto, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion ){
        return dto.getReplacedStreamId() == null
            ? streamEntityDao.save(DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), xPagopaPnApiVersion, dto))
            : replaceStream(xPagopaPnCxId,xPagopaPnCxGroups,xPagopaPnApiVersion, dto);
    }

    @Override
    public Mono<Void> deleteEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "deleteEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId ={} ";
        String[] args = {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, streamId.toString()};

        generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).log();

        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .flatMap(filteredEntity ->
                 streamEntityDao.delete(xPagopaPnCxId, streamId.toString())
                    .then(Mono.fromSupplier(() -> {
                        schedulerService.scheduleWebhookEvent(streamId.toString(), null, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM);
                        return null;
            })))
            .doOnSuccess(empty-> generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).generateSuccess().log()).doOnError(err-> generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).generateFailure("error deleting stream", err).log()).then()
            ;
    }

    @Override
    public Mono<StreamMetadataResponseV23> getEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "getEventStream xPagopaPnUid={}, xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={} ";
        List<String> args = Arrays.asList(xPagopaPnUid, xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, streamId.toString());
        generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).log();

        return getStreamEntityToRead(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .map(EntityToDtoStreamMapper::entityToDto)
            .doOnSuccess(entity->
                generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateSuccess().log()
            )
            .doOnError(err-> generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateFailure("error getting stream", err).log());
    }

    @Override
    public Flux<StreamListElement> listEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion) {
        String msg = "listEventStream xPagopaPnUid={}, xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={} ";
        List<String> args = Arrays.asList(xPagopaPnUid, xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion);
        generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).log();

        return streamEntityDao.findByPa(xPagopaPnCxId)
            .map(EntityToStreamListDtoStreamMapper::entityToDto)
            .doOnComplete(()->
                generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateSuccess().log()
            )
            .doOnError(err-> generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateFailure("error listing streams", err).log());
    }

    @Override
    public Mono<StreamMetadataResponseV23> updateEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV23> streamRequest) {
        String msg = "updateEventStream xPagopaPnUid={},xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={}, request={} ";
        List<String> args = Arrays.asList(xPagopaPnUid, xPagopaPnCxId, groupString(xPagopaPnCxGroups), streamId.toString(), xPagopaPnApiVersion);

        return streamRequest.doOnNext(payload-> {
            List<String> values = new ArrayList<>(args);
            values.add(payload.toString());
            generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, values.toArray(new String[0])).log();
        }).flatMap(request ->
            getStreamEntityToRead(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .then(streamRequest)
            .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), xPagopaPnApiVersion, r))
            .map(entity -> {
                entity.setEventAtomicCounter(null);
                return entity;
            })
            .flatMap(streamEntityDao::update)
            .map(EntityToDtoStreamMapper::entityToDto)
        ).doOnSuccess(newEntity-> generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, args.toArray(new String[0])).generateSuccess().log()).doOnError(err-> generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, args.toArray(new String[0])).generateFailure("error updating stream", err).log());
    }

    @Override
    public Mono<StreamMetadataResponseV23> disableEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}";
        String[] args = new String[] {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();

        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .filter(streamEntity -> streamEntity.getDisabledDate() == null)
            .switchIfEmpty(
                Mono.error(new PnWebhookForbiddenException("Not supported operation, stream already disabled"))
            ).flatMap(streamEntity->
                streamEntityDao.disable(streamEntity).map(EntityToDtoStreamMapper::entityToDto)
            ).doOnSuccess( ok->
                generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateSuccess().log()
            ).doOnError(err ->
                generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateFailure("Error in disableEventStream").log());
    }

    private Mono<Boolean> checkStreamCount(String xPagopaPnCxId){
        return streamEntityDao.findByPa(xPagopaPnCxId)
            .filter(streamEntity -> streamEntity.getDisabledDate() == null)
                .collectList().flatMap(list -> {
                    if (list.size() >= pnDeliveryPushConfigs.getWebhook().getMaxStreams()) {
                        return Mono.error(new PnWebhookMaxStreamsCountReachedException());
                    }
                    else {
                        return Mono.just(Boolean.TRUE);
                    }
                });
    }

    private Mono<StreamEntity> replaceStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, StreamCreationRequestV23 dto){
        StreamEntity streamEntity = DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), xPagopaPnApiVersion, dto);
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, disabledStreamId={}";
        String[] args = new String[] {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, dto.getReplacedStreamId().toString()};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();
        return replaceStreamEntity(streamEntity, dto.getReplacedStreamId()).doOnSuccess(newEntity-> generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateSuccess().log()).doOnError(err-> generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateFailure(ERROR_CREATING_STREAM, err).log());
    }

    private Mono<StreamEntity> replaceStreamEntity(StreamEntity entity, UUID replacedStreamId) {
        return streamEntityDao.get(entity.getPaId(), replacedStreamId.toString())
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Not supported operation, replace stream invalid")))
            .filter(foundEntity-> foundEntity.getDisabledDate() == null)
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Not supported operation, stream already disabled")))
            .flatMap(foundEntity-> {
                StreamEntity replacedEntity = new StreamEntity(foundEntity.getPaId(), foundEntity.getStreamId());
                return streamEntityDao.replaceEntity(replacedEntity, entity);
            });
    }

}
