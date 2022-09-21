package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsPrivateApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.utils.LegalFactUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class PnInternalLegalFactsController implements LegalFactsPrivateApi {

    private final GetLegalFactService getLegalFactService;

    public PnInternalLegalFactsController(GetLegalFactService getLegalFactService) {
        this.getLegalFactService = getLegalFactService;
    }

    @Override
    public Mono<ResponseEntity<LegalFactDownloadMetadataResponse>> getLegalFactPrivate(
            String xPagopaPnCxId,
            String iun,
            LegalFactCategory legalFactType,
            String legalFactId,
            String mandateId,
            ServerWebExchange exchange) {

        return Mono.fromSupplier(() ->
                ResponseEntity.ok(getLegalFactService.getLegalFactMetadata(iun, legalFactType, legalFactId, xPagopaPnCxId, mandateId ))
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalFactListElement>>> getNotificationLegalFactsPrivate(
            String xPagopaPnCxId,
            String iun,
            String mandateId,
            ServerWebExchange exchange) {

        return Mono.fromSupplier(() -> {
            List<LegalFactListElement> legalFacts = getLegalFactService.getLegalFacts(iun, xPagopaPnCxId, mandateId);
            Flux<LegalFactListElement> fluxFacts = Flux.fromStream(legalFacts.stream().map(LegalFactUtils::convert));
            return ResponseEntity.ok(fluxFacts);
        });
    }
}
