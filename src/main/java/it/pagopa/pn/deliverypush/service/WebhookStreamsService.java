package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WebhookStreamsService {
    Mono<StreamMetadataResponseV23> createEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV23> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Mono<StreamMetadataResponseV23> getEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion);

    Mono<StreamMetadataResponseV23> updateEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV23> streamRequest);

    Mono<StreamMetadataResponseV23> disableEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);
}
