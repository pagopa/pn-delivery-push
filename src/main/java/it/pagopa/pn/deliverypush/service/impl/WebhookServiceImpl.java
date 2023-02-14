package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookService;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final StreamEntityDao streamEntityDao;
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final WebhookUtils webhookUtils;
    private final int retryAfter;
    private final int purgeDeletionWaittime;

    private final int maxStreams;

    public WebhookServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao,
                              PnDeliveryPushConfigs pnDeliveryPushConfigs, SchedulerService schedulerService,
                              WebhookUtils webhookUtils) {
        this.streamEntityDao = streamEntityDao;
        this.eventEntityDao = eventEntityDao;
        this.webhookUtils = webhookUtils;
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.retryAfter = webhookConf.getScheduleInterval().intValue();
        this.purgeDeletionWaittime = webhookConf.getPurgeDeletionWaittime();
        this.schedulerService = schedulerService;
        this.maxStreams= webhookConf.getMaxStreams();
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
        // grazie al contatore atomico usato in scrittura per generare l'eventId, non serve più gestire la finestra.
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId)))
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId))
                .map(res -> {
                    List<ProgressResponseElement> eventList = res.getEvents().stream().map(this::mapEntityToDto).sorted(Comparator.comparing(ProgressResponseElement::getEventId)).toList();

                    log.info("consumeEventStream requestEventId={} streamId={} size={} returnedlastEventId={}", lastEventId, streamId, eventList.size(), (!eventList.isEmpty()?eventList.get(eventList.size()-1).getEventId():"ND"));
                    // schedulo la pulizia per gli eventi precedenti a quello richiesto
                    schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventId, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM_OLDER_THAN);
                    // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                    return ProgressResponseElementDto.builder()
                            .retryAfter(res.getLastEventIdRead() == null ? retryAfter : 0)
                            .progressResponseElementList(eventList)
                            .build();
                });
    }

    @NotNull
    private ProgressResponseElement mapEntityToDto(EventEntity ev) {
        ProgressResponseElement progressResponseElement = new ProgressResponseElement();
        progressResponseElement.setEventId(ev.getEventId());
        progressResponseElement.setTimestamp(ev.getTimestamp());
        progressResponseElement.setIun(ev.getIun());
        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatus.valueOf(ev.getNewStatus()) : null);
        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
        progressResponseElement.setTimelineEventCategory(TimelineElementCategory.fromValue(ev.getTimelineEventCategory()));
        progressResponseElement.setChannel(ev.getChannel());
        progressResponseElement.setRecipientIndex(ev.getRecipientIndex());
        progressResponseElement.setLegalfactIds(ev.getLegalfactIds());
        progressResponseElement.setAnalogCost(ev.getAnalogCost());
        return progressResponseElement;
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
                        return Mono.fromSupplier(() -> webhookUtils.retrieveTimeline(iun, timelineId))
                                .map(timelineData -> Tuples.of(l, timelineData.getEvent(), timelineData.getNotificationInt(),
                                        timelineData.getNotificationStatusUpdate().getOldStatus().getValue(), timelineData.getNotificationStatusUpdate().getNewStatus().getValue()));
                    }
                })
                .flatMapMany(res -> {
                    String oldStatus = res.getT4();
                    String newStatus = res.getT5();
                    return Flux.fromIterable(res.getT1())
                            .flatMap(stream -> processEvent(stream, oldStatus, newStatus, res.getT2(), res.getT3()));
                }).collectList().then();
    }

    private Mono<Void> processEvent(StreamEntity stream,  String oldStatus, String newStatus, TimelineElementInternal timelineElementInternal, NotificationInt notificationInt) {
        // per ogni stream configurato, devo andare a controllare se lo stato devo salvarlo o meno
        // c'è il caso in cui lo stato non cambia (e se lo stream vuolo solo i cambi di stato, lo ignoro)
        if (!StringUtils.hasText(stream.getEventType()))
        {
            log.warn("skipping saving because webhook stream configuration is not correct stream={}", stream);
            return Mono.empty();
        }

        StreamCreationRequest.EventTypeEnum eventType = StreamCreationRequest.EventTypeEnum.fromValue(stream.getEventType());
        if (eventType == StreamCreationRequest.EventTypeEnum.STATUS
                && newStatus.equals(oldStatus))
        {
            log.info("skipping saving webhook event for stream={} because old and new status are same status={} iun={}", stream.getStreamId(), newStatus, timelineElementInternal.getIun());
            return Mono.empty();
        }

        String timelineEventCategory = timelineElementInternal.getCategory().getValue();
        // e poi c'è il caso in cui lo stream ha un filtro sugli eventi interessati
        // se è nullo/vuoto o contiene lo stato, vuol dire che devo salvarlo
        if ((stream.getFilterValues() == null
                || stream.getFilterValues().isEmpty()
                || (eventType == StreamCreationRequest.EventTypeEnum.STATUS && stream.getFilterValues().contains(newStatus))
                || (eventType == StreamCreationRequest.EventTypeEnum.TIMELINE && stream.getFilterValues().contains(timelineEventCategory))))
        {
            return saveEventWithAtomicIncrement(stream, newStatus, timelineElementInternal, notificationInt);
        }
        else {
            log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={} iun={}", stream.getStreamId(), timelineEventCategory, timelineElementInternal.getIun());
        }

        return Mono.empty();
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


    private Mono<Void> saveEventWithAtomicIncrement(StreamEntity streamEntity, String newStatus,
                                                    TimelineElementInternal timelineElementInternal, NotificationInt notificationInt){
        // recupero un contatore aggiornato
        return streamEntityDao.updateAndGetAtomicCounter(streamEntity)
            .flatMap(atomicCounterUpdated -> {
                if (atomicCounterUpdated < 0)
                {
                    log.warn("updateAndGetAtomicCounter counter is -1, skipping saving stream");
                    return Mono.empty();
                }

                return eventEntityDao.save(webhookUtils.buildEventEntity(atomicCounterUpdated, streamEntity,
                                                                            newStatus, timelineElementInternal, notificationInt))
                        .doOnSuccess(event -> log.info("saved webhookevent={}", event))
                        .then();
            });
    }
}
