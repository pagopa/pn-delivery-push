package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestv23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponsev23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamUpdateRequestv23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<StreamMetadataResponsev23> createEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestv23> streamCreationRequest) {
        return streamCreationRequest
            .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), r))
            .flatMap(dto -> streamEntityDao.findByPa(xPagopaPnCxId)
                .collectList().flatMap(list -> {
                    if (list.size() >= maxStreams) {
                        return Mono.error(new PnWebhookMaxStreamsCountReachedException());
                    }
                    else {
                        return Mono.empty();
                    }
                }).thenReturn(dto))
            .flatMap(streamEntityDao::save)
            .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<Void> deleteEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {

        //IVAN: qui dobbiamo inserire check su permessi gruppo?
        return filterEntity(xPagopaPnCxId,xPagopaPnCxGroups,streamId)
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " groups (" + String.join(",",xPagopaPnCxGroups)+ ") is not allowed to see this streamId " + streamId)))
            .flatMap(filteredEntity ->
                 streamEntityDao.delete(xPagopaPnCxId, streamId.toString())
                    .then(Mono.fromSupplier(() -> {
                        schedulerService.scheduleWebhookEvent(streamId.toString(), null, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM);
                        return null;
            })));
    }

    @Override
    public Mono<StreamMetadataResponsev23> getEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
            .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Flux<StreamListElement> listEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion) {
        return streamEntityDao.findByPa(xPagopaPnCxId)
            .map(EntityToStreamListDtoStreamMapper::entityToDto);
    }

    //IVAN: Qui abbiamo introdotto lo StreamUpdateRequest ... prima veniva utilizzato StreamCreationRequest, corretto?
    @Override
    public Mono<StreamMetadataResponsev23> updateEventStream(String xPagopaPnCxId,
        List<String> xPagopaPnCxGroups,
        String xPagopaPnApiVersion,
        UUID streamId,
        Mono<StreamUpdateRequestv23> streamUpdateRequest) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " is not allowed to update this streamId " + streamId)))
            .then(streamUpdateRequest)
            .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), r))
            .map(entity -> {
                entity.setEventAtomicCounter(null);
                return entity;
            })
            .flatMap(streamEntityDao::update)
            .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<StreamMetadataResponsev23> disableEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        throw new NotImplementedException("da fare!");
    }

}
