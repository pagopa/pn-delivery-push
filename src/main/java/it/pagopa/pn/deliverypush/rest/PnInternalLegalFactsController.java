package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsPrivateApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.utils.LegalFactUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
public class PnInternalLegalFactsController implements LegalFactsPrivateApi {

    private final GetLegalFactService getLegalFactService;

    public PnInternalLegalFactsController(GetLegalFactService getLegalFactService) {
        this.getLegalFactService = getLegalFactService;
    }

    @Override
    public Mono<ResponseEntity<LegalFactDownloadMetadataResponse>> getLegalFactPrivate(
            String recipientInternalId,
            String iun,
            LegalFactCategory legalFactType,
            String legalFactId,
            String mandateId,
            CxTypeAuthFleet xPagopaPnCxType,
            List<String> xPagopaPnCxGroups,
            ServerWebExchange exchange) {

        return Mono.fromSupplier(() -> {
                    log.debug("Start getLegalFactPrivate for iun={} recipientInternalId={}", iun, recipientInternalId);
                    return ResponseEntity.ok(getLegalFactService.getLegalFactMetadata(iun, legalFactType, legalFactId, recipientInternalId, mandateId, xPagopaPnCxType, xPagopaPnCxGroups));
                }
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalFactListElement>>> getNotificationLegalFactsPrivate(
            String recipientInternalId,
            String iun,
            String mandateId,
            CxTypeAuthFleet cxType,
            List<String> cxGroups,
            ServerWebExchange exchange) {

        return Mono.fromSupplier(() -> {
            log.debug("Start getNotificationLegalFactsPrivate - iun={} recipientInternalId={}", iun, recipientInternalId);
            List<LegalFactListElement> legalFacts = getLegalFactService.getLegalFacts(iun, recipientInternalId, mandateId, cxType, cxGroups);
            Flux<LegalFactListElement> fluxFacts = Flux.fromStream(legalFacts.stream().map(LegalFactUtils::convert));
            return ResponseEntity.ok(fluxFacts);
        });
    }
}
