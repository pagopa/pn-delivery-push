package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnNotificationCancelledException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElementV20;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.utils.LegalFactUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
public class PnLegalFactsController implements LegalFactsApi {

    private final GetLegalFactService getLegalFactService;
    private final TimelineUtils timelineUtils;


    public PnLegalFactsController(GetLegalFactService getLegalFactService, TimelineUtils timelineUtils) {
        this.getLegalFactService = getLegalFactService;
        this.timelineUtils = timelineUtils;
    }

    @Override
    @Deprecated
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
        if (! CxTypeAuthFleet.PA.equals(xPagopaPnCxType) && timelineUtils.checkIsNotificationCancellationRequested(iun)
            && !timelineUtils.checkIsNotificationCancelledLegalFactId(iun, legalFactId)){
            log.warn("Notification already cancelled, returning 404 iun={} ", iun);
            throw new PnNotificationCancelledException();
        }else {
            return getLegalFactService.getLegalFactMetadata(iun, legalFactType, legalFactId, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups)
                .map(response -> ResponseEntity.ok().body(response));
        }
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
        if (! CxTypeAuthFleet.PA.equals(xPagopaPnCxType) && timelineUtils.checkIsNotificationCancellationRequested(iun)
                && !timelineUtils.checkIsNotificationCancelledLegalFactId(iun, legalFactId) ){
            log.warn("Notification already cancelled, returning 404 iun={} ", iun);
            throw new PnNotificationCancelledException();
        }else {
            return getLegalFactService.getLegalFactMetadata(iun, null, legalFactId, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups)
                .map(response -> ResponseEntity.ok().body(response));
        }
    }
    
    @Override
    public Mono<ResponseEntity<Flux<LegalFactListElementV20>>> getNotificationLegalFactsV20(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            String iun,
            List<String> xPagopaPnCxGroups,
            UUID mandateId,
            ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            List<LegalFactListElementV20> legalFacts = getLegalFactService.getLegalFacts(iun, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups);
            Flux<LegalFactListElementV20> fluxFacts = Flux.fromStream(legalFacts.stream().map(LegalFactUtils::convert));
            return ResponseEntity.ok(fluxFacts);
        });
    }
}
