package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV25;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV25;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV25;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface WebhookStreamsService {
    Mono<StreamMetadataResponseV25> createEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV25> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Mono<StreamMetadataResponseV25> getEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion);

    Mono<StreamMetadataResponseV25> updateEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV25> streamRequest);

    Mono<StreamMetadataResponseV25> disableEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);
}
