package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

                if (StringUtils.isNotBlank(t2.getT2())){
                    disableEventStream(xPagopaPnCxId,xPagopaPnCxGroups,xPagopaPnApiVersion,UUID.fromString(t2.getT2()));
                }
                return streamEntityDao.findByPa(xPagopaPnCxId)
                .collectList().flatMap(list -> {
                    if (list.size() >= maxStreams) {
                        return Mono.error(new PnWebhookMaxStreamsCountReachedException());
                    }
                    else {
                        return Mono.empty();
                    }
                }).thenReturn(t2.getT1());
            })
            .flatMap(streamEntityDao::save)
            .map(EntityToDtoStreamMapper::entityToDto);
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
            .filter(streamEntity -> streamEntity.getDisabledDate() == null)
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Not supported operation, stream already disabled")))
            .flatMap(filteredEntity ->{
                filteredEntity.setDisabledDate(Instant.now());
                filteredEntity.setTtl(10000L + pnDeliveryPushConfigs.getDisableTtl());//TODO: IVAN: "attuale scadenza degli eventi"?
                return streamEntityDao.save(filteredEntity);
            }).map(EntityToDtoStreamMapper::entityToDto);
    }

}
