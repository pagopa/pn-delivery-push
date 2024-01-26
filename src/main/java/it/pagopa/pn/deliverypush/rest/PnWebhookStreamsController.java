package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.StreamsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
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
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] createEventStream xPagopaPnCxId={}", xPagopaPnCxId);

        return MDCUtils.addMDCToContextAndExecute(
                webhookService.createEventStream(xPagopaPnCxId, streamCreationRequest)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] deleteEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                webhookService.deleteEventStream(xPagopaPnCxId, streamId)
                        .then(Mono.just(ResponseEntity.noContent().build()))
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponse>> getEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] getEventStream xPagopaPnCxId={} streamId={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                webhookService.getEventStream(xPagopaPnCxId, streamId)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<StreamListElement>>> listEventStreams(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, ServerWebExchange exchange) {
        log.info("[enter] listEventStreams xPagopaPnCxId={}", xPagopaPnCxId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(webhookService.listEventStream(xPagopaPnCxId)));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponse>> updateEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, Mono<StreamCreationRequest> streamCreationRequest, ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] updateEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                webhookService.updateEventStream(xPagopaPnCxId, streamId, streamCreationRequest)
                        .map(ResponseEntity::ok)
        );
    }
}
