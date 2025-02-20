package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV27;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV26;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV26;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface WebhookStreamsService {
    Mono<StreamMetadataResponseV26> createEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV27> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Mono<StreamMetadataResponseV26> getEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion);

    Mono<StreamMetadataResponseV26> updateEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV26> streamRequest);

    Mono<StreamMetadataResponseV26> disableEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);
}
