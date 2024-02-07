package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WebhookStreamsServiceImpl extends WebhookServiceImpl implements WebhookStreamsService {

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
        String msg = "createEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}";
        String[] args = {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion};

        return streamCreationRequest.doOnNext(payload-> {
                String[] fullArgs = ArrayUtils.add(args, payload.toString());
                generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg+", request={} ", fullArgs).log();
            }).flatMap(x->
                (x.getReplacedStreamId() == null ? checkStreamCount(xPagopaPnCxId) : Mono.just(Boolean.TRUE)).then(Mono.just(x))
            )
            .flatMap(dto -> {
                List<String> allowedGroups = (xPagopaPnCxGroups==null || xPagopaPnCxGroups.isEmpty())
                    ? pnExternalRegistryClient.getGroups(xPagopaPnUid, xPagopaPnCxId)
                    : xPagopaPnCxGroups;

                return WebhookUtils.checkGroups(dto.getGroups(), allowedGroups)?
                    (dto.getReplacedStreamId() == null)
                        ? streamEntityDao.save(DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(),dto))
                        : replaceStream(xPagopaPnCxId,xPagopaPnCxGroups,xPagopaPnApiVersion, dto)
                    : Mono.error(new PnWebhookForbiddenException("Not Allowed groups "+groupString(dto.getGroups()))); //TODO: IVAN, vedere tutti i messaggi
            }).map(EntityToDtoStreamMapper::entityToDto).doOnSuccess(newEntity->{
                generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg, args).generateSuccess().log();
            }).doOnError(err->{
                generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg, args).generateFailure("error creating stream", err).log();
            });
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
            .doOnSuccess(empty->{
                generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).generateSuccess().log();
            }).doOnError(err->{
                generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).generateFailure("error deleting stream", err).log();
            }).then()
            ;
    }

    @Override
    public Mono<StreamMetadataResponseV23> getEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
            .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Flux<StreamListElement> listEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion) {
        return streamEntityDao.findByPa(xPagopaPnCxId)
            .map(EntityToStreamListDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<StreamMetadataResponseV23> updateEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV23> streamRequest) {
        String msg = "updateEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={}, request={} ";
        List<String> args = Arrays.asList(new String[]{xPagopaPnCxId, groupString(xPagopaPnCxGroups), streamId.toString(), xPagopaPnApiVersion});

        return streamRequest.doOnNext(payload-> {
            List<String> values = new ArrayList<>();
            values.addAll(args);
            values.add(payload.toString());
            generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, values.toArray(new String[0])).log();
        }).flatMap(request ->
            getStreamEntityToRead(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .then(streamRequest)
            .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), r))
            .map(entity -> {
                entity.setEventAtomicCounter(null);
                return entity;
            })
            .flatMap(streamEntityDao::update)
            .map(EntityToDtoStreamMapper::entityToDto)
        ).doOnSuccess(newEntity->{
            generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, args.toArray(new String[0])).generateSuccess().log();
        }).doOnError(err->{
            generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, args.toArray(new String[0])).generateFailure("error updating stream", err).log();
        });
    }

    @Override
    public Mono<StreamMetadataResponseV23> disableEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}";
        String[] args = new String[] {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();

        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .filter(streamEntity -> {
                return streamEntity.getDisabledDate() == null;
            })
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
                .collectList().flatMap(list -> {
                    if (list.size() >= pnDeliveryPushConfigs.getWebhook().getMaxStreams()) {
                        return Mono.error(new PnWebhookMaxStreamsCountReachedException());
                    }
                    else {
                        return Mono.just(Boolean.TRUE);
                    }
                });
    }

    private String groupString(List<String> groups){
        return groups==null ? null : String.join(",",groups);
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog( PnAuditLogEventType pnAuditLogEventType, String message, String[] arguments) {
        PnAuditLogEvent pnAuditLogEvent = null;
        String logMessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{}", logMessage)
            .build();
        return logEvent;
    }

    private String getApiVersion(String xPagopaPnApiVersion){
        return StringUtils.isNotBlank(xPagopaPnApiVersion) ? xPagopaPnApiVersion : pnDeliveryPushConfigs.getWebhook().getCurrentVersion();
    }

    private Mono<StreamEntity> replaceStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, StreamEntity streamEntity, UUID replacedStreamId){
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, disabledStreamId={}";
        String[] args = new String[] {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, replacedStreamId.toString()};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();
        return streamEntityDao.replace(streamEntity, replacedStreamId).doOnSuccess(newEntity->{
            generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateSuccess().log();
        }).doOnError(err->{
            generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateFailure("error creating stream", err).log();
        });
    }

    private Mono<StreamEntity> replaceStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, StreamCreationRequestV23 dto){
        StreamEntity streamEntity = DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), dto);
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, disabledStreamId={}";
        String[] args = new String[] {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, dto.getReplacedStreamId().toString()};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();
        return streamEntityDao.replace(streamEntity, dto.getReplacedStreamId()).doOnSuccess(newEntity->{
            generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateSuccess().log();
        }).doOnError(err->{
            generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateFailure("error creating stream", err).log();
        });
    }

}
