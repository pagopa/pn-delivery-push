package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface WebhookService {
    Mono<StreamMetadataResponse> createEventStream(String xPagopaPnCxId, Mono<StreamCreationRequest> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnCxId, UUID streamId);

    Mono<StreamMetadataResponse> getEventStream(String xPagopaPnCxId, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnCxId);

    Mono<StreamMetadataResponse> updateEventStream(String xPagopaPnCxId, UUID streamId, Mono<StreamCreationRequest> streamCreationRequest);

    Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, UUID streamId, String lastEventId);

    Mono<Void> saveEvent(String streamId, String eventId, String iun, String requestId, Instant timestamp, String newStatus,
                         String timelineEventCategory);

    Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan);
}
