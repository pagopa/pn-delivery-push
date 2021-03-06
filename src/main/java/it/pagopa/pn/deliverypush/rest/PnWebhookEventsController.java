package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.EventsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.StreamsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;
import it.pagopa.pn.deliverypush.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
public class PnWebhookEventsController implements EventsApi {

    public static final String HEADER_RETRY_AFTER = "retry-after";
    private final WebhookService webhookService;

    public PnWebhookEventsController(WebhookService webhookService) { this.webhookService = webhookService; }

    @Override
    public Mono<ResponseEntity<Flux<ProgressResponseElement>>> consumeEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, String lastEventId, ServerWebExchange exchange) {
        log.info("[enter] getEventStream xPagopaPnCxId={} uuid={} lastEventID={}", xPagopaPnCxId, streamId.toString(), lastEventId);
        return webhookService.consumeEventStream(xPagopaPnCxId, streamId, lastEventId)
                .map(r -> {
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.set(HEADER_RETRY_AFTER,
                            ""+r.getRetryAfter());

                    return ResponseEntity
                            .ok()
                            .headers(responseHeaders)
                            .body(Flux.fromIterable(r.getProgressResponseElementList()));
                });
    }

    @Override
    public Mono<ResponseEntity<Void>> informOnExternalEvent(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Mono<ExternalEventsRequest> externalEventsRequest, ServerWebExchange exchange) {
        log.error("[enter] informOnExternalEvent not implemented yet");
        return Mono.just(ResponseEntity.internalServerError().build());
    }
}
