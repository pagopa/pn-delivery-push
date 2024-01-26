package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestv23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponsev23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamUpdateRequestv23;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WebhookStreamsService {
    Mono<StreamMetadataResponsev23> createEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestv23> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Mono<StreamMetadataResponsev23> getEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion);

    Mono<StreamMetadataResponsev23> updateEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamUpdateRequestv23> streamUpdateRequest);

    Mono<StreamMetadataResponsev23> disableEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);
}
