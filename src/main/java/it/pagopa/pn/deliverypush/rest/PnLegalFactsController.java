package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.utils.LegalFactUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
public class PnLegalFactsController implements LegalFactsApi {

    private final GetLegalFactService getLegalFactService;

    public PnLegalFactsController(GetLegalFactService getLegalFactService) {
        this.getLegalFactService = getLegalFactService;
    }

    @Override
    public Mono<ResponseEntity<LegalFactDownloadMetadataResponse>> getLegalFact(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType, 
            String xPagopaPnCxId,
            String iun,
            LegalFactCategory legalFactType,
            String legalFactId,
            List<String> xPagopaPnCxGroups,
            UUID mandateId,
            ServerWebExchange exchange) {
        return getLegalFactService.getLegalFactMetadata(iun, legalFactType, legalFactId, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups)
                .map(response -> ResponseEntity.ok().body(response));
    }


    @Override
    public Mono<ResponseEntity<LegalFactDownloadMetadataResponse>> getLegalFactById(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            String iun,
            String legalFactId,
            List<String> xPagopaPnCxGroups,
            UUID mandateId,
            ServerWebExchange exchange) {
        return getLegalFactService.getLegalFactMetadata(iun, null, legalFactId, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups)
                .map(response -> ResponseEntity.ok().body(response));
    }
    
    @Override
    public Mono<ResponseEntity<Flux<LegalFactListElement>>> getNotificationLegalFacts(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            String iun,
            List<String> xPagopaPnCxGroups,
            UUID mandateId,
            ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            List<LegalFactListElement> legalFacts = getLegalFactService.getLegalFacts(iun, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups);
            Flux<LegalFactListElement> fluxFacts = Flux.fromStream(legalFacts.stream().map(LegalFactUtils::convert));
            return ResponseEntity.ok(fluxFacts);
        });
    }
}
