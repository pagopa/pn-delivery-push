package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_CONSUMEEVENTSTREAM;

@Service
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final StreamEntityDao streamEntityDao;
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final int retryAfter;
    private final int purgeDeletionWaittime;
    private final int readBufferDelay;

    public WebhookServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao, PnDeliveryPushConfigs pnDeliveryPushConfigs, SchedulerService schedulerService) {
        this.streamEntityDao = streamEntityDao;
        this.eventEntityDao = eventEntityDao;
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.retryAfter = webhookConf.getScheduleInterval().intValue();
        this.purgeDeletionWaittime = webhookConf.getPurgeDeletionWaittime().intValue();
        this.readBufferDelay = webhookConf.getReadBufferDelay();
        this.schedulerService = schedulerService;
    }

    @Override
    public Mono<StreamMetadataResponse> createEventStream(String xPagopaPnCxId, Mono<StreamCreationRequest> streamCreationRequest) {
        return streamCreationRequest
                .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), r))
                .flatMap(streamEntityDao::save)
                .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<Void> deleteEventStream(String xPagopaPnCxId, UUID streamId) {
        return streamEntityDao.delete(xPagopaPnCxId, streamId.toString())
                .then(Mono.fromSupplier(() -> {
                    schedulerService.scheduleWebhookEvent(streamId.toString(), null, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM);
                    return null;
                }));
    }

    @Override
    public Mono<StreamMetadataResponse> getEventStream(String xPagopaPnCxId, UUID streamId) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Flux<StreamListElement> listEventStream(String xPagopaPnCxId) {
        return streamEntityDao.findByPa(xPagopaPnCxId)
                .map(EntityToStreamListDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<StreamMetadataResponse> updateEventStream(String xPagopaPnCxId, UUID streamId, Mono<StreamCreationRequest> streamCreationRequest) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnInternalException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_UPDATEEVENTSTREAM)))
                .then(streamCreationRequest)
                .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), r))
                .flatMap(streamEntityDao::save)
                .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, UUID streamId, String lastEventId) {
        // per gestire i casi in cui una PA viene a chiedere gli eventi più nuovi di X, glieli ritorno e 1ms DOPO viene scritto un nuovo evento con timestamp più "vecchio" (X -1ms tipo, causa ritardo nella creazione/elaborazione)
        // quell'evento  verrebbe perso, perchè alla richiesta successiva ritornerei solo quelli più NUOVI di X. Sfruttando la struttura del lastEventId, vado a ricalcolare un istante temporale antecedente
        // che permette di ritornare anche il record con (X-1ms) se presente. NB: vado a filtrare il record con lastEventId perchè quello SICURAMENTE glielo avevo già tornato.
        String lastEventIdWithBuffer = computeLastEventIdWithBuffer(lastEventId);
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnInternalException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId, ERROR_CODE_WEBHOOK_CONSUMEEVENTSTREAM)))
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventIdWithBuffer))
                .map(res -> {
                    // schedulo la pulizia per gli eventi precedenti a quello richiesto
                    schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventIdWithBuffer, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM_OLDER_THAN);
                    // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                    return ProgressResponseElementDto.builder()
                            .retryAfter(res.getLastEventIdRead() == null ? retryAfter : 0)
                            .progressResponseElementList(res.getEvents().stream().filter(ev -> !ev.getEventId().equals(lastEventId)).map(ev -> {
                                ProgressResponseElement progressResponseElement = new ProgressResponseElement();
                                progressResponseElement.setEventId(ev.getEventId());
                                progressResponseElement.setTimestamp(ev.getTimestamp());
                                progressResponseElement.setIun(ev.getIun());
                                progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatus.valueOf(ev.getNewStatus()) : null);
                                progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
                                progressResponseElement.setTimelineEventCategory(TimelineElementCategory.fromValue(ev.getTimelineEventCategory()));
                                return progressResponseElement;
                            }).collect(Collectors.toList()))
                            .build();
                })
                ;
    }

    @Override
    public Mono<Void> saveEvent(String paId, String eventId, String iun, Instant timestamp,
                                String oldStatus, String newStatus, String timelineEventCategory) {
        return streamEntityDao.findByPa(paId)
                .map(stream -> {
                    // per ogni stream configurato, devo andare a controllare se lo stato devo salvarlo o meno
                    // c'è il caso in cui lo stato non cambia (e se lo stream vuolo solo i cambi di stato, lo ignoro)
                    StreamCreationRequest.EventTypeEnum eventType = StreamCreationRequest.EventTypeEnum.fromValue(stream.getEventType());
                    if (eventType == StreamCreationRequest.EventTypeEnum.STATUS
                        && newStatus.equals(oldStatus))
                    {
                        log.info("skipping saving webhook event for stream={} because old and new status are same status={}", stream.getStreamId(), newStatus);
                        return Mono.empty();
                    }

                    // e poi c'è il caso in cui lo stream ha un filtro sugli eventi interessati
                    // se è nullo/vuoto o contiene lo stato, vuol dire che devo salvarlo
                    if ((stream.getFilterValues() == null
                            || stream.getFilterValues().isEmpty()
                            || (eventType == StreamCreationRequest.EventTypeEnum.STATUS && stream.getFilterValues().contains(newStatus))
                            || (eventType == StreamCreationRequest.EventTypeEnum.TIMELINE && stream.getFilterValues().contains(timelineEventCategory))))
                    {

                        EventEntity eventEntity = new EventEntity();
                        eventEntity.setEventId(eventId);
                        eventEntity.setTimestamp(timestamp);
                        // Lo iun ci va solo se è stata accettata, quindi escludo gli stati invalidation e refused
                        if (StringUtils.hasText(newStatus)
                            && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.IN_VALIDATION
                            && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.REFUSED)
                            eventEntity.setIun(iun);
                        eventEntity.setNewStatus(newStatus);
                        eventEntity.setTimelineEventCategory(timelineEventCategory);
                        eventEntity.setStreamId(stream.getStreamId());
                        // il requestId ci va sempre, ed è il base64 dello iun
                        eventEntity.setNotificationRequestId(Base64Utils.encodeToString(iun.getBytes(StandardCharsets.UTF_8)));
                        return eventEntityDao.save(eventEntity).then();
                    }
                    else {
                        log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={}", stream.getStreamId(), timelineEventCategory);
                        return Mono.empty();
                    }
                })
                .collectList().then();
    }



    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan)
    {
        log.info("purgeEvents streamId={} eventId={} olderThan={}", streamId, eventId, olderThan);
        return eventEntityDao.delete(streamId, eventId, olderThan)
                .map(thereAreMore -> {
                    if (thereAreMore)
                    {
                        log.info("purgeEvents streamId={} eventId={} olderThan={} there are more event to purge", streamId, eventId, olderThan);
                        schedulerService.scheduleWebhookEvent(streamId, eventId, purgeDeletionWaittime, olderThan?WebhookEventType.PURGE_STREAM_OLDER_THAN:WebhookEventType.PURGE_STREAM);
                    }
                    else
                        log.info("purgeEvents streamId={} eventId={} olderThan={} no more event to purge", streamId, eventId, olderThan);

                    return thereAreMore;
                })
                .then();
    }

    private String computeLastEventIdWithBuffer(String lastEventId){
        // se readBuffedDelay  è minore di 0, di fatto è disabilitato
        if (readBufferDelay <= 0)
            return lastEventId;

        if (StringUtils.hasText(lastEventId))
        {
            String timestamp = lastEventId.split("_")[0];
            return Instant.parse(timestamp).minusMillis(readBufferDelay).toString();
        }
        return null;
    }
}
