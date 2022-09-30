package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.PaperNotificationFailedApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class PnPaperNotificationFailedController implements PaperNotificationFailedApi {

    private final PaperNotificationFailedService service;

    public PnPaperNotificationFailedController(PaperNotificationFailedService service) {
        this.service = service;
    }

    @Override
    public Mono<ResponseEntity<Flux<ResponsePaperNotificationFailedDto>>> paperNotificationFailed(
            String recipientInternalId,
            Boolean getAAR,
            final ServerWebExchange exchange
    ) {
        return Mono.fromSupplier(() -> {
            List<ResponsePaperNotificationFailedDto> responses = service.getPaperNotificationByRecipientId(recipientInternalId, getAAR);
            Flux<ResponsePaperNotificationFailedDto> fluxFacts = Flux.fromIterable(responses);
            return ResponseEntity.ok(fluxFacts);
        });
    }

}
