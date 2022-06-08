package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final StreamEntityDao streamEntityDao;
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final int retryAfter;
    private final int purgeDeletionWaittime;

    public WebhookServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao, PnDeliveryPushConfigs pnDeliveryPushConfigs, SchedulerService schedulerService) {
        this.streamEntityDao = streamEntityDao;
        this.eventEntityDao = eventEntityDao;
        this.retryAfter = pnDeliveryPushConfigs.getWebhook().getScheduleInterval().intValue();
        this.purgeDeletionWaittime = pnDeliveryPushConfigs.getWebhook().getPurgeDeletionWaittime().intValue();
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
                    schedulerService.scheduleWebhookEvent(streamId.toString(), null, Instant.now().plusMillis(purgeDeletionWaittime), WebhookEventType.PURGE_STREAM);
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
        return streamCreationRequest
                .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), r))
                .flatMap(streamEntityDao::save)
                .map(EntityToDtoStreamMapper::entityToDto);
    }

    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, UUID streamId, String lastEventId) {
        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnInternalException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId)))
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId))
                .map(res -> {
                    // schedulo la pulizia per gli eventi precedenti a quello richiesto
                    schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventId, Instant.now().plusMillis(purgeDeletionWaittime), WebhookEventType.PURGE_STREAM_OLDER_THAN);
                    return ProgressResponseElementDto.builder()
                            .retryAfter(res.getLastEventIdRead() == null ? retryAfter : 0)
                            .progressResponseElementList(res.getEvents().stream().map(ev -> {
                                ProgressResponseElement progressResponseElement = new ProgressResponseElement();
                                progressResponseElement.setEventId(ev.getTimestamp().toString());
                                progressResponseElement.setTimestamp(ev.getTimestamp());
                                progressResponseElement.setIun(ev.getIun());
                                progressResponseElement.setNewStatus(NotificationStatus.fromValue(ev.getNewStatus()));
                                progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
                                progressResponseElement.setTimelineEventCategory(TimelineElementCategory.fromValue(ev.getTimelineEventCategory()));
                                return progressResponseElement;
                            }).collect(Collectors.toList()))
                            .build();
                })
                ;
    }

    @Override
    public Mono<Void> saveEvent(String streamId, String eventId, String iun, String requestId, Instant timestamp, String newStatus, String timelineEventCategory) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(eventId);
        eventEntity.setTimestamp(timestamp);
        eventEntity.setIun(iun);
        eventEntity.setNewStatus(newStatus);
        eventEntity.setTimelineEventCategory(timelineEventCategory);
        eventEntity.setStreamId(streamId);
        eventEntity.setNotificationRequestId(requestId);
        return eventEntityDao.save(eventEntity).then();
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
                        schedulerService.scheduleWebhookEvent(streamId, eventId, Instant.now().plusMillis(purgeDeletionWaittime), olderThan?WebhookEventType.PURGE_STREAM_OLDER_THAN:WebhookEventType.PURGE_STREAM);
                    }
                    else
                        log.info("purgeEvents streamId={} eventId={} olderThan={} no more event to purge", streamId, eventId, olderThan);

                    return null;
                })
                .onErrorResume(e -> {
                    log.error("purgeEvents throws error, rescheduling streamId={} eventId={} olderThan={}", streamId, eventId, olderThan, e);
                    schedulerService.scheduleWebhookEvent(streamId, eventId, Instant.now().plusMillis(purgeDeletionWaittime), olderThan?WebhookEventType.PURGE_STREAM_OLDER_THAN:WebhookEventType.PURGE_STREAM);
                    return Mono.empty();
                })
                .then();
    }
}
