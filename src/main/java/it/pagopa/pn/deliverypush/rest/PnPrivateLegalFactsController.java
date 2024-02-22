package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnNotificationCancelledException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsPrivateApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataWithContentTypeResponse;
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
public class PnPrivateLegalFactsController implements LegalFactsPrivateApi {

    private final GetLegalFactService getLegalFactService;
    private final TimelineUtils timelineUtils;

    public PnPrivateLegalFactsController(GetLegalFactService getLegalFactService, TimelineUtils timelineUtils) {
        this.getLegalFactService = getLegalFactService;
        this.timelineUtils = timelineUtils;
    }

    @Override
    public Mono<ResponseEntity<LegalFactDownloadMetadataWithContentTypeResponse>> getLegalFactPrivate(
            String recipientInternalId,
            String iun,
            LegalFactCategory legalFactType,
            String legalFactId,
            String mandateId,
            CxTypeAuthFleet xPagopaPnCxType,
            List<String> xPagopaPnCxGroups,
            ServerWebExchange exchange) {
        log.info("Starting getLegalFact (private) Process");

        if (! CxTypeAuthFleet.PA.equals(xPagopaPnCxType) && timelineUtils.checkIsNotificationCancellationRequested(iun)){
            log.warn("Notification already cancelled, returning 404 iun={} ", iun);
            throw new PnNotificationCancelledException();
        }else {
            return getLegalFactService.getLegalFactMetadataWithContentType(iun, legalFactType, legalFactId, recipientInternalId, mandateId, xPagopaPnCxType, xPagopaPnCxGroups)
                .doOnSuccess(res -> log.info("Starting getLegalFact (private) Process"))
                .map(response -> ResponseEntity.ok().body(response));
        }
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalFactListElement>>> getNotificationLegalFactsPrivate(
            String recipientInternalId,
            String iun,
            String mandateId,
            CxTypeAuthFleet cxType,
            List<String> cxGroups,
            ServerWebExchange exchange) {
        log.info("Starting getNotificationLegalFacts (private) Process");

        if (! CxTypeAuthFleet.PA.equals(cxType) && timelineUtils.checkIsNotificationCancellationRequested(iun)){
            log.warn("Notification already cancelled, returning 404 iun={} ", iun);
            throw new PnNotificationCancelledException();
        }else {
            return Mono.fromSupplier(() -> {
                log.debug("Start getNotificationLegalFactsPrivate - iun={} recipientInternalId={}", iun, recipientInternalId);
                List<LegalFactListElement> legalFacts = getLegalFactService.getLegalFacts(iun, recipientInternalId, mandateId, cxType, cxGroups);
                Flux<LegalFactListElement> fluxFacts = Flux.fromStream(legalFacts.stream().map(LegalFactUtils::convert));
                return ResponseEntity.ok(fluxFacts);
            });
        }
    }
}
