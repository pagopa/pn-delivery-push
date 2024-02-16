package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.webhook.EventTimelineInternalDto;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.TimelineElementV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.WebhookEventsService;
import it.pagopa.pn.deliverypush.service.mapper.ProgressResponseElementMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineElementWebhookMapper;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static it.pagopa.pn.deliverypush.service.utils.WebhookUtils.checkGroups;


@Service
@Slf4j
public class WebhookEventsServiceImpl extends WebhookServiceImpl implements WebhookEventsService {
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final WebhookUtils webhookUtils;
    private final TimelineService timelineService;
    private final ConfidentialInformationService confidentialInformationService;
    private final Set<String> defaultNotificationStatuses;
    private static final String DEFAULT_CATEGORIES = "DEFAULT";


    public WebhookEventsServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao,
                                    SchedulerService schedulerService, WebhookUtils webhookUtils,
                                    PnDeliveryPushConfigs pnDeliveryPushConfigs, TimelineService timeLineService,
                                    ConfidentialInformationService confidentialInformationService) {
        super(streamEntityDao, pnDeliveryPushConfigs);
        this.eventEntityDao = eventEntityDao;
        this.schedulerService = schedulerService;
        this.webhookUtils = webhookUtils;
        this.timelineService = timeLineService;
        this.confidentialInformationService = confidentialInformationService;
        this.defaultNotificationStatuses = statusByVersion(NotificationStatusInt.VERSION_10);
    }

    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId,
        List<String> xPagopaPnCxGroups,
        String xPagopaPnApiVersion,
        UUID streamId,
        String lastEventId) {
        // grazie al contatore atomico usato in scrittura per generare l'eventId, non serve più gestire la finestra.
        return getStreamEntityToRead(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId, xPagopaPnCxGroups, streamId)
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId))
                .flatMap(res ->
                    toEventTimelineInternalFromEventEntity(res.getEvents())
                            .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)))
                            //timeline ancora anonimizzato - EventEntity + TimelineElementInternal
                            .collectList()
                            // chiamo timelineService per aggiungere le confidentialInfo
                            .flatMapMany(items -> {
                                if (webhookUtils.getVersion(xPagopaPnApiVersion) == 10)
                                    return Flux.fromStream(items.stream());
                                return addConfidentialInformationAtEventTimelineList(items);
                            })
                            // converto l'eventTimelineInternalDTO in ProgressResponseElementV23
                            .map(this::getProgressResponseFromEventTimeline)
                            .sort(Comparator.comparing(ProgressResponseElementV23::getEventId))
                            .collectList()
                            .map(eventList -> {
                                var retryAfter = pnDeliveryPushConfigs.getWebhook().getScheduleInterval().intValue();
                                int currentRetryAfter = res.getLastEventIdRead() == null ? retryAfter : 0;
                                var purgeDeletionWaittime = pnDeliveryPushConfigs.getWebhook().getPurgeDeletionWaittime();
                                log.info("consumeEventStream lastEventId={} streamId={} size={} returnedlastEventId={} retryAfter={}", lastEventId, streamId, eventList.size(), (!eventList.isEmpty()?eventList.get(eventList.size()-1).getEventId():"ND"), currentRetryAfter);
                                // schedulo la pulizia per gli eventi precedenti a quello richiesto
                                schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventId, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM_OLDER_THAN);
                                // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                                return ProgressResponseElementDto.builder()
                                        .retryAfter(currentRetryAfter)
                                        .progressResponseElementList(eventList)
                                        .build();
                            })
                );
    }

    private ProgressResponseElementV23 getProgressResponseFromEventTimeline(EventTimelineInternalDto eventTimeline) {
        var response = ProgressResponseElementMapper.internalToExternalv23(eventTimeline.getEventEntity());
        if (StringUtils.hasText(eventTimeline.getEventEntity().getElement())) {
            TimelineElementV23 timelineElementV23 = TimelineElementWebhookMapper.internalToExternal(eventTimeline.getTimelineElementInternal());
            response.setElement(timelineElementV23);
        }
        return response;
    }

    private Flux<EventTimelineInternalDto> toEventTimelineInternalFromEventEntity(List<EventEntity> events) throws PnInternalException{
        return Flux.fromStream(events.stream())
                .map(item -> {
                    TimelineElementInternal timelineElementInternal = getTimelineInternalFromEventEntity(item);
                    return EventTimelineInternalDto.builder()
                            .eventEntity(item)
                            .timelineElementInternal(timelineElementInternal)
                            .build();
                });
    }

    private TimelineElementInternal getTimelineInternalFromEventEntity(EventEntity entity) throws PnInternalException{
        if (StringUtils.hasText(entity.getElement())) {
            return webhookUtils.getTimelineInternalFromEvent(entity);
        }
        return null;
    }

    @Override
    public Mono<Void> saveEvent(String paId, String timelineId, String iun) {
        return streamEntityDao.findByPa(paId)
                .filter(entity -> entity.getDisabledDate() == null)
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

        if (!CollectionUtils.isEmpty(stream.getGroups()) && !checkGroups(Collections.singletonList(notificationInt.getGroup()), stream.getGroups())){
            return Mono.empty();
        }
        // per ogni stream configurato, devo andare a controllare se lo stato devo salvarlo o meno
        // c'è il caso in cui lo stato non cambia (e se lo stream vuolo solo i cambi di stato, lo ignoro)
        if (!StringUtils.hasText(stream.getEventType()))
        {
            log.warn("skipping saving because webhook stream configuration is not correct stream={}", stream);
            return Mono.empty();
        }

        StreamCreationRequestV23.EventTypeEnum eventType = StreamCreationRequestV23.EventTypeEnum.fromValue(stream.getEventType());
        if (eventType == StreamCreationRequestV23.EventTypeEnum.STATUS
            && newStatus.equals(oldStatus))
        {
            log.info("skipping saving webhook event for stream={} because old and new status are same status={} iun={}", stream.getStreamId(), newStatus, timelineElementInternal.getIun());
            return Mono.empty();
        }

        String timelineEventCategory = timelineElementInternal.getCategory().getValue();

        Set<String> filteredValues = new LinkedHashSet<>();
        if (eventType == StreamCreationRequestV23.EventTypeEnum.TIMELINE) {
            filteredValues = categoriesByFilter(stream);
        } else if (eventType == StreamCreationRequestV23.EventTypeEnum.STATUS){
            filteredValues = stream.getFilterValues() == null || stream.getFilterValues().isEmpty()
                ? defaultNotificationStatuses
                : stream.getFilterValues();
        }

        // e poi c'è il caso in cui lo stream ha un filtro sugli eventi interessati
        // se è nullo/vuoto o contiene lo stato, vuol dire che devo salvarlo
        if ( (eventType == StreamCreationRequestV23.EventTypeEnum.STATUS && filteredValues.contains(newStatus))
            || (eventType == StreamCreationRequestV23.EventTypeEnum.TIMELINE && filteredValues.contains(timelineEventCategory)))
        {
            return saveEventWithAtomicIncrement(stream, newStatus, timelineElementInternal);
        }
        else {
            log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={} iun={}", stream.getStreamId(), timelineEventCategory, timelineElementInternal.getIun());
        }

        return Mono.empty();
    }

    private Mono<Void> saveEventWithAtomicIncrement(StreamEntity streamEntity, String newStatus,
        TimelineElementInternal timelineElementInternal){
        // recupero un contatore aggiornato
        return streamEntityDao.updateAndGetAtomicCounter(streamEntity)
            .flatMap(atomicCounterUpdated -> {
                if (atomicCounterUpdated < 0)
                {
                    log.warn("updateAndGetAtomicCounter counter is -1, skipping saving stream");
                    return Mono.empty();
                }

                return eventEntityDao.save(webhookUtils.buildEventEntity(atomicCounterUpdated, streamEntity,
                        newStatus, timelineElementInternal))
                        .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)))
                    .doOnSuccess(event -> log.info("saved webhookevent={}", event))
                    .then();
            });
    }
    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan) {
        log.info("purgeEvents streamId={} eventId={} olderThan={}", streamId, eventId, olderThan);
        return eventEntityDao.delete(streamId, eventId, olderThan)
            .map(thereAreMore -> {
                if (Boolean.TRUE.equals(thereAreMore))
                {
                    var purgeDeletionWaittime = pnDeliveryPushConfigs.getWebhook().getPurgeDeletionWaittime();
                    log.info("purgeEvents streamId={} eventId={} olderThan={} there are more event to purge", streamId, eventId, olderThan);
                    schedulerService.scheduleWebhookEvent(streamId, eventId, purgeDeletionWaittime, olderThan?WebhookEventType.PURGE_STREAM_OLDER_THAN:WebhookEventType.PURGE_STREAM);
                }
                else
                    log.info("purgeEvents streamId={} eventId={} olderThan={} no more event to purge", streamId, eventId, olderThan);

                return thereAreMore;
            })
            .then();
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

    private Set<String> categoriesByFilter(StreamEntity stream) {
        Set<String> categoriesSet;
        if (CollectionUtils.isEmpty(stream.getFilterValues())){
            categoriesSet = categoriesByVersion(webhookUtils.getVersion(stream.getVersion()));
        } else {
            categoriesSet = stream.getFilterValues().stream()
                    .filter(v -> !v.equalsIgnoreCase(DEFAULT_CATEGORIES))
                    .collect(Collectors.toSet());
            if (stream.getFilterValues().contains(DEFAULT_CATEGORIES)) {
                categoriesSet.addAll(pnDeliveryPushConfigs.getListCategoriesPa());
            }
        }
        return categoriesSet;
    }


    private Flux<EventTimelineInternalDto> addConfidentialInformationAtEventTimelineList(List<EventTimelineInternalDto> eventEntities) {
        List<TimelineElementInternal> timelineElementInternals = eventEntities.stream()
                .map(EventTimelineInternalDto::getTimelineElementInternal)
                .filter(Objects::nonNull)
                .toList();

        return this.confidentialInformationService.getTimelineConfidentialInformation(timelineElementInternals)
                .map(confidentialInfo -> {
                    // cerco l'elemento in TimelineElementInternals con elementiId
                    TimelineElementInternal internal = timelineElementInternals.stream()
                            .filter(i -> i.getElementId().equals(confidentialInfo.getTimelineElementId()))
                            .findFirst()
                            .get();
                    timelineService.enrichTimelineElementWithConfidentialInformation(internal.getDetails(), confidentialInfo);
                    return internal;
                })
                .collectList()
                .flatMapMany(item -> Flux.fromStream(eventEntities.stream()));
    }
}