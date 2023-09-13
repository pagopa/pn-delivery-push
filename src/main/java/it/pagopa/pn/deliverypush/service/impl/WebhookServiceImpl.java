package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookMaxStreamsCountReachedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponse;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookService;
import it.pagopa.pn.deliverypush.service.mapper.ProgressResponseElementMapper;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

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

    private final Set<String> defaultCategories;
    private final Set<String> defaultNotificationStatuses;

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
        defaultCategories = categoriesByVersion(TimelineElementCategoryInt.VERSION_10);
        defaultNotificationStatuses = statusByVersion(NotificationStatusInt.VERSION_10);
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
                .map(entity -> {
                    entity.setEventAtomicCounter(null);
                    return entity;
                })
                .flatMap(streamEntityDao::update)
                .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, UUID streamId, String lastEventId) {
        // grazie al contatore atomico usato in scrittura per generare l'eventId, non serve più gestire la finestra.
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId)))
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId))
                .map(res -> {
                    List<ProgressResponseElement> eventList = res.getEvents().stream().map(ProgressResponseElementMapper::internalToExternal).sorted(Comparator.comparing(ProgressResponseElement::getEventId)).toList();

                    int currentRetryAfter = res.getLastEventIdRead() == null ? retryAfter : 0;

                    log.info("consumeEventStream requestEventId={} streamId={} size={} returnedlastEventId={} retryAfter={}", lastEventId, streamId, eventList.size(), (!eventList.isEmpty()?eventList.get(eventList.size()-1).getEventId():"ND"), currentRetryAfter);
                    // schedulo la pulizia per gli eventi precedenti a quello richiesto
                    schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventId, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM_OLDER_THAN);
                    // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                    return ProgressResponseElementDto.builder()
                            .retryAfter(currentRetryAfter)
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

        Set<String> filteredValues = new LinkedHashSet<>();
        if (eventType == StreamCreationRequest.EventTypeEnum.TIMELINE) {
            filteredValues = stream.getFilterValues()== null || stream.getFilterValues().isEmpty()
                ? defaultCategories
                : stream.getFilterValues();
        } else if (eventType == StreamCreationRequest.EventTypeEnum.STATUS){
            filteredValues = stream.getFilterValues() == null || stream.getFilterValues().isEmpty()
                ? defaultNotificationStatuses
                : stream.getFilterValues();
        }

        // e poi c'è il caso in cui lo stream ha un filtro sugli eventi interessati
        // se è nullo/vuoto o contiene lo stato, vuol dire che devo salvarlo
        if ( (eventType == StreamCreationRequest.EventTypeEnum.STATUS && filteredValues.contains(newStatus))
                || (eventType == StreamCreationRequest.EventTypeEnum.TIMELINE && filteredValues.contains(timelineEventCategory)))
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

    private Set<String> categoriesByVersion(int version) {
        return Arrays.stream(TimelineElementCategoryInt.values())
                .filter( e -> e.getVersion() <= version)
                .map(TimelineElementCategoryInt::getValue)
                .collect(Collectors.toSet());
    }

    private Set<String> statusByVersion(int version) {
        return Arrays.stream(NotificationStatusInt.values())
                .filter( e -> e.getVersion() <= version)
                .map(NotificationStatusInt::getValue)
                .collect(Collectors.toSet());
    }
}
