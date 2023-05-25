package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.StreamsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponse;
import it.pagopa.pn.deliverypush.service.WebhookService;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
public class PnWebhookStreamsController implements StreamsApi {

    private final WebhookService webhookService;

    public PnWebhookStreamsController(WebhookService webhookService) { this.webhookService = webhookService; }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponse>> createEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Mono<StreamCreationRequest> streamCreationRequest, ServerWebExchange exchange) {
        log.info("[enter] createEventStream xPagopaPnCxId={}", xPagopaPnCxId);
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        return webhookService.createEventStream(xPagopaPnCxId, streamCreationRequest)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, ServerWebExchange exchange) {
        log.info("[enter] deleteEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        return webhookService.deleteEventStream(xPagopaPnCxId, streamId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponse>> getEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, ServerWebExchange exchange) {
        log.info("[enter] getEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        return webhookService.getEventStream(xPagopaPnCxId, streamId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<StreamListElement>>> listEventStreams(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, ServerWebExchange exchange) {
        log.info("[enter] listEventStreams xPagopaPnCxId={}", xPagopaPnCxId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(webhookService.listEventStream(xPagopaPnCxId)));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponse>> updateEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, Mono<StreamCreationRequest> streamCreationRequest, ServerWebExchange exchange) {
        log.info("[enter] updateEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        return webhookService.updateEventStream(xPagopaPnCxId, streamId, streamCreationRequest)
                .map(ResponseEntity::ok);
    }
}
