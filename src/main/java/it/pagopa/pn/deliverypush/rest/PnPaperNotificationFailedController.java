package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.PaperNotificationFailedApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnPaperNotificationFailedController implements PaperNotificationFailedApi {

    private final PaperNotificationFailedService service;

    @Override
    public Mono<ResponseEntity<Flux<ResponsePaperNotificationFailedDto>>> paperNotificationFailed(
            String recipientInternalId,
            Boolean getAAR,
            final ServerWebExchange exchange
    ) {

        return Mono.fromSupplier(() -> {
            var responses = service.getPaperNotificationByRecipientId(recipientInternalId, getAAR);
            var fluxFacts = Flux.fromIterable(responses);
            return ResponseEntity.ok(fluxFacts);
        });
    }

}
