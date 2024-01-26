package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WebhookService {
    Mono<StreamMetadataResponseV23> createEventStream(String xPagopaPnCxId, Mono<StreamCreationRequestV23> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnCxId, UUID streamId);

    Mono<StreamMetadataResponseV23> getEventStream(String xPagopaPnCxId, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnCxId);

    Mono<StreamMetadataResponseV23> updateEventStream(String xPagopaPnCxId, UUID streamId, Mono<StreamCreationRequestV23> streamCreationRequest);

    Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, UUID streamId, String lastEventId);

    Mono<Void> saveEvent(String paId, String eventId, String iun);

    Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan);
}
