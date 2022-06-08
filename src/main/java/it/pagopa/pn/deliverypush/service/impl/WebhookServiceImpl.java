package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.deliverypush.service.WebhookService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

public class WebhookServiceImpl implements WebhookService {

    private final StreamEntityDao streamEntityDao;
    private final EventEntityDao eventEntityDao;
    private final int retryAfter;

    public WebhookServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.streamEntityDao = streamEntityDao;
        this.eventEntityDao = eventEntityDao;
        this.retryAfter = pnDeliveryPushConfigs.getWebhook().getScheduleInterval().intValue();
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
        return streamEntityDao.delete(xPagopaPnCxId, streamId.toString());
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
                .switchIfEmpty(Mono.error(new PnInternalException("Pa " + xPagopaPnCxId + " is not allowed to see this streamId " + streamId.toString())))
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), Instant.parse(lastEventId)))
                .map(res -> ProgressResponseElementDto.builder()
                        .retryAfter(res.getLastTimestampRead()==null?retryAfter:0)
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
                        .build());
    }
}
