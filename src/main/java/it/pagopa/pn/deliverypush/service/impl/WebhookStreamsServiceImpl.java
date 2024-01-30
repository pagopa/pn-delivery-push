package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Service
@Slf4j
public class WebhookStreamsServiceImpl extends WebhookServiceImpl implements WebhookStreamsService {

    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public WebhookStreamsServiceImpl (StreamEntityDao streamEntityDao, SchedulerService schedulerService, PnDeliveryPushConfigs pnDeliveryPushConfigs){
        super(streamEntityDao);
        this.schedulerService = schedulerService;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    private int maxStreams;
    private int purgeDeletionWaittime;

    @PostConstruct
    private void postConstruct() {
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.maxStreams = webhookConf.getScheduleInterval().intValue();
        this.purgeDeletionWaittime = webhookConf.getPurgeDeletionWaittime();
    }
    @Override
    public Mono<StreamMetadataResponseV23> createEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV23> streamCreationRequest) {

        return streamCreationRequest
            .map(r -> Tuples.of (DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), r)
                , r.getReplacedStreamId() != null ? r.getReplacedStreamId().toString() : ""))
            .flatMap(t2 -> {
                return WebhookUtils.checkGroups(t2.getT1().getGroups(),xPagopaPnCxGroups)?
                    (StringUtils.isBlank(t2.getT2()) ? streamEntityDao.save(t2.getT1()) : streamEntityDao.replace(t2.getT1(), t2.getT2()))
                    : Mono.error(new PnWebhookForbiddenException("Not Allowed groups "+groupString(t2.getT1().getGroups()))); //TODO: IVAN, vedere tutti i messaggi
            }).map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<Void> deleteEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {

        return filterEntity(xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .flatMap(filteredEntity ->
                 streamEntityDao.delete(xPagopaPnCxId, streamId.toString())
                    .then(Mono.fromSupplier(() -> {
                        schedulerService.scheduleWebhookEvent(streamId.toString(), null, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM);
                        return null;
            })));
    }

    @Override
    public Mono<StreamMetadataResponseV23> getEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
            .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Flux<StreamListElement> listEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion) {
        return streamEntityDao.findByPa(xPagopaPnCxId)
            .map(EntityToStreamListDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<StreamMetadataResponseV23> updateEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV23> streamRequest) {

        return filterEntity(xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .then(streamRequest)
            .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), r))
            .map(entity -> {
                entity.setEventAtomicCounter(null);
                return entity;
            })
            .flatMap(streamEntityDao::update)
            .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<StreamMetadataResponseV23> disableEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        return filterEntity(xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .filter(streamEntity -> {
                return streamEntity.getDisabledDate() == null;
            })
            .switchIfEmpty(
                Mono.error(new PnWebhookForbiddenException("Not supported operation, stream already disabled"))
            ).flatMap(streamEntity->
                streamEntityDao.disable(streamEntity).map(EntityToDtoStreamMapper::entityToDto)
            );
    }

    private String groupString(List<String> groups){
        return String.join(",",groups);
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(StreamEntity streamEntity) {
        PnAuditLogEvent pnAuditLogEvent = null;
        return pnAuditLogEvent;
    }
}
