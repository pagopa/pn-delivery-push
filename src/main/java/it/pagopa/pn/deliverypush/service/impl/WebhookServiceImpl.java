package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
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
import it.pagopa.pn.deliverypush.service.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.swing.text.html.Option;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final StreamEntityDao streamEntityDao;
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final TimelineService timelineService;
    private final StatusService statusService;
    private final NotificationService notificationService;
    private final int retryAfter;
    private final int purgeDeletionWaittime;
    private final int readBufferDelay;
    private final int maxStreams;
    private final Duration ttl;

    public WebhookServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao, PnDeliveryPushConfigs pnDeliveryPushConfigs, SchedulerService schedulerService, TimelineService timelineService, StatusService statusService, NotificationService notificationService) {
        this.streamEntityDao = streamEntityDao;
        this.eventEntityDao = eventEntityDao;
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.retryAfter = webhookConf.getScheduleInterval().intValue();
        this.purgeDeletionWaittime = webhookConf.getPurgeDeletionWaittime().intValue();
        this.readBufferDelay = webhookConf.getReadBufferDelay();
        this.schedulerService = schedulerService;
        this.timelineService = timelineService;
        this.statusService = statusService;
        this.notificationService = notificationService;
        this.maxStreams= webhookConf.getMaxStreams();
        this.ttl = webhookConf.getTtl();
    }

    @Override
    public Mono<StreamMetadataResponse> createEventStream(String xPagopaPnCxId, Mono<StreamCreationRequest> streamCreationRequest) {
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
                .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " is not allowed to update this streamId " + streamId)))
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
        String lastEventIdWithBuffer = computeLastEventIdWithBufferForSearch(lastEventId);
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId)))
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventIdWithBuffer))
                .map(res -> {
                    List<ProgressResponseElement> eventList = res.getEvents().stream().filter(ev -> !ev.getEventId().equals(lastEventId)).map(ev -> {
                        ProgressResponseElement progressResponseElement = new ProgressResponseElement();
                        progressResponseElement.setEventId(ev.getEventId());
                        progressResponseElement.setTimestamp(ev.getTimestamp());
                        progressResponseElement.setIun(ev.getIun());
                        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatus.valueOf(ev.getNewStatus()) : null);
                        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
                        progressResponseElement.setTimelineEventCategory(TimelineElementCategory.fromValue(ev.getTimelineEventCategory()));
                        return progressResponseElement;
                    }).collect(Collectors.toList());

                    // schedulo la pulizia per gli eventi precedenti a quello richiesto
                    String lastEventIdWithBufferForPurge = computeLastEventIdWithBufferForPurge(lastEventId, !eventList.isEmpty()?eventList.get(0).getEventId():null);
                    schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventIdWithBufferForPurge, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM_OLDER_THAN);
                    // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                    return ProgressResponseElementDto.builder()
                            .retryAfter(res.getLastEventIdRead() == null ? retryAfter : 0)
                            .progressResponseElementList(eventList)
                            .build();
                });
    }

    @Override
    public Mono<Void> saveEvent(String paId, String timelineId, String iun) {
        return streamEntityDao.findByPa(paId)
                .collectList()
                .flatMap(l -> {
                    if (l.isEmpty()) {
                        return Mono.empty();    // se non ho stream in ascolto, non c'è motivo di fare le query in dynamo
                    }
                    else {
                        return Mono.fromSupplier(() -> retrieveTimeline(iun, timelineId))
                                .map(timelineData -> new Object() {
                                    public final List<StreamEntity> streamList = l;
                                    public final TimelineElementInternal timelineElementInternal = timelineData.getEvent();
                                    public final String oldStatus = timelineData.getNotificationStatusUpdate().getOldStatus().getValue();
                                    public final String newStatus = timelineData.getNotificationStatusUpdate().getNewStatus().getValue();
                                });
                    }
                })
                .flatMapMany(res -> {
                    Instant timestamp = res.timelineElementInternal.getTimestamp();
                    String oldStatus = res.oldStatus;
                    String newStatus = res.newStatus;
                    String timelineEventCategory = res.timelineElementInternal.getCategory().getValue();
                    return Flux.fromIterable(res.streamList)
                            .flatMap(stream -> processEvent(stream, timestamp, oldStatus, newStatus, timelineEventCategory, timelineId, iun));
                }).collectList().then();
    }

    private Mono<Void> processEvent(StreamEntity stream, Instant timestamp, String oldStatus, String newStatus, String timelineEventCategory, String timelineId, String iun) {
        // per ogni stream configurato, devo andare a controllare se lo stato devo salvarlo o meno
        // c'è il caso in cui lo stato non cambia (e se lo stream vuolo solo i cambi di stato, lo ignoro)
        StreamCreationRequest.EventTypeEnum eventType = StreamCreationRequest.EventTypeEnum.fromValue(stream.getEventType());
        if (eventType == StreamCreationRequest.EventTypeEnum.STATUS
                && newStatus.equals(oldStatus))
        {
            log.info("skipping saving webhook event for stream={} because old and new status are same status={} iun={}", stream.getStreamId(), newStatus, iun);
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
            if (!ttl.isZero())
                eventEntity.setTtl(LocalDateTime.now().plus(ttl).atZone(ZoneId.systemDefault()).toEpochSecond());

            eventEntity.setEventId(timestamp.toString() + "_" + timelineId);
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
            log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={} iun={}", stream.getStreamId(), timelineEventCategory, iun);
        }

        return Mono.empty();
    }

    private RetrieveTimelineResult retrieveTimeline(String iun, String timelineId) {
        NotificationInt notificationInt = notificationService.getNotificationByIun(iun);
        Set<TimelineElementInternal> timelineElementInternalSet = timelineService.getTimeline(iun, false);
        Optional<TimelineElementInternal> event = timelineElementInternalSet.stream().filter(x -> x.getElementId().equals(timelineId)).findFirst();

        if (event.isEmpty())
            throw new PnInternalException("Timeline event not found in timeline history", PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_SAVEEVENT);

        // considero gli elementi di timeline più vecchi di quello passato
        Set<TimelineElementInternal> filteredPreviousTimelineElementInternalSet = timelineElementInternalSet.stream().filter(x -> x.getTimestamp().isBefore(event.get().getTimestamp())).collect(Collectors.toSet());
        // calcolo vecchio e nuovo stato in base allo storico "di quel momento"
        StatusService.NotificationStatusUpdate notificationStatusUpdate = statusService.computeStatusChange(event.get(), filteredPreviousTimelineElementInternalSet, notificationInt);
        return RetrieveTimelineResult.builder()
                .event(event.get())
                .notificationStatusUpdate(notificationStatusUpdate)
                .build();
    }

    @Builder
    @Getter
    private static class RetrieveTimelineResult{
        private StatusService.NotificationStatusUpdate notificationStatusUpdate;
        private TimelineElementInternal event;
    }


    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan)
    {
        log.info("purgeEvents streamId={} eventId={} olderThan={}", streamId, eventId, olderThan);
        return eventEntityDao.delete(streamId, eventId, olderThan)
                .map(thereAreMore -> {
                    if (Boolean.TRUE.equals(thereAreMore))
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

    /**
     * Modificata la logica. Questo metodo viene invocato quando devo chiedere gli eventi più nuovi del timestamp tornato. In questo caso riceverò probabilmente un eventId nel passato
     *
     * @param lastEventId ultimo evento su cui basarsi per ricavare il timestamp
     * @return un timestamp sotto forma di stringa
     */
    private String computeLastEventIdWithBufferForSearch(String lastEventId){
        // se readBuffedDelay  è minore di 0, di fatto è disabilitato
        if (readBufferDelay <= 0)
            return lastEventId;

        if (StringUtils.hasText(lastEventId))
        {
            // ritorno gli eventi più nuovi dell'eventId meno una piccola finestra temporale, per recuperare eventuali eventi ricevuti precedentemente ma non tornati per eventuali corse critiche.
            String timestamp = lastEventId.split("_")[0];
            return Instant.parse(timestamp).minusMillis(readBufferDelay).toString();
        }
        return null;
    }

    /**
     * Questo metodo viene invocato  per dover cancellare gli eventi più vecchi del timestamp tornato. Anche in questo caso riceverò l'eventId relativo alla data confermata dall'utente, ma anche la data dell'evento più vecchio letto da DB.
     * Se l'evento più vecchio letto da DB è ANTECEDENTE a quello confermato, vuol dire che non glielo avevo dato la volta precedente, e quindi lo ritorno.
     * Se invece l'evento più vecchio non c'è o è POSTERIORE a quello confermato, posso tranquillamente cancellare FINO a quello confermato INCLUSO.
     *
     * @param lastEventIdCONFIRMED ultimo evento CONFERMATO su cui basarsi per ricavare il timestamp
     * @param lastEventIdOLDEST ultimo evento LETTO DA DB su cui basarsi per ricavare il timestamp
     * @return un timestamp sotto forma di stringa
     */
    private String computeLastEventIdWithBufferForPurge(String lastEventIdCONFIRMED, String lastEventIdOLDEST){
        // se readBuffedDelay  è minore di 0, di fatto è disabilitato
        if (readBufferDelay <= 0)
            return lastEventIdCONFIRMED;

        if (StringUtils.hasText(lastEventIdCONFIRMED))
        {
            if (StringUtils.hasText(lastEventIdOLDEST))
            {
                String timestampCONFIRMED = lastEventIdCONFIRMED.split("_")[0];
                String timestampOLDEST = lastEventIdOLDEST.split("_")[0];
                Instant instantCONFIRMED = Instant.parse(timestampCONFIRMED);
                Instant instantOLDEST = Instant.parse(timestampOLDEST);
                if (instantOLDEST.isBefore(instantCONFIRMED))
                {
                    return instantOLDEST.minusMillis(readBufferDelay).toString();
                }
            }

            return lastEventIdCONFIRMED;
        }
        return null;
    }
}
