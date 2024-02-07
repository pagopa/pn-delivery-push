package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.WebhookEventsService;
import it.pagopa.pn.deliverypush.service.mapper.ProgressResponseElementMapper;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventsServiceImpl implements WebhookEventsService {
    private final StreamEntityDao streamEntityDao;
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final WebhookUtils webhookUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private int retryAfter;
    private int purgeDeletionWaittime;
    private Set<String> defaultCategories;
    private Set<String> defaultNotificationStatuses;

    @PostConstruct
    private void postConstruct() {
        defaultCategories = categoriesByVersion(TimelineElementCategoryInt.VERSION_10);
        defaultNotificationStatuses = statusByVersion(NotificationStatusInt.VERSION_10);
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.retryAfter = webhookConf.getScheduleInterval().intValue();
        this.purgeDeletionWaittime = webhookConf.getPurgeDeletionWaittime();
    }

    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId,
        List<String> xPagopaPnCxGroups,
        String xPagopaPnApiVersion,
        UUID streamId,
        String lastEventId) {
        // grazie al contatore atomico usato in scrittura per generare l'eventId, non serve più gestire la finestra.
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId)))
            .flatMap(stream -> {
                verifyVersion(xPagopaPnCxGroups, xPagopaPnApiVersion, stream);
                return eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId);
            })
            .map(res -> {

                List<ProgressResponseElementV23> eventList = res.getEvents().stream()
                        .filter(eventEntity -> eventEntity.getTimestamp() != null) // Filtra solo gli eventi con timestamp non nulli
                        .map(ProgressResponseElementMapper::internalToExternalv23)
                        .sorted(Comparator.comparing(ProgressResponseElementV23::getEventId))
                        .toList();

                int currentRetryAfter = res.getLastEventIdRead() == null ? retryAfter : 0;

                log.info("consumeEventStream lastEventId={} streamId={} size={} returnedlastEventId={} retryAfter={}", lastEventId, streamId, eventList.size(), (!eventList.isEmpty()?eventList.get(eventList.size()-1).getEventId():"ND"), currentRetryAfter);
                // schedulo la pulizia per gli eventi precedenti a quello richiesto
                schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventId, purgeDeletionWaittime, WebhookEventType.PURGE_STREAM_OLDER_THAN);
                // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                return ProgressResponseElementDto.builder()
                    .retryAfter(currentRetryAfter)
                    .progressResponseElementList(eventList)
                    .build();
            });
    }

    private static void verifyVersion(List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, StreamEntity stream) {

        if (StringUtils.equals(xPagopaPnApiVersion, "V10") && !StringUtils.equals(stream.getVersion(), "V10") ){
                Mono.error(new PnWebhookForbiddenException("Not supported operation"));
        }
        else {
            if (!WebhookUtils.checkGroups(stream.getGroups(), xPagopaPnCxGroups)){
                Mono.error(new PnWebhookForbiddenException("Not supported operation"));
            }
        }
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
        if (!org.springframework.util.StringUtils.hasText(stream.getEventType()))
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
            filteredValues = stream.getFilterValues()== null || stream.getFilterValues().isEmpty()
                ? defaultCategories
                : stream.getFilterValues();
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
            return saveEventWithAtomicIncrement(stream, newStatus, timelineElementInternal, notificationInt);
        }
        else {
            log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={} iun={}", stream.getStreamId(), timelineEventCategory, timelineElementInternal.getIun());
        }

        return Mono.empty();
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
    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan) {
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
