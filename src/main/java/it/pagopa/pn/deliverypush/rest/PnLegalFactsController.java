package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class PnLegalFactsController implements LegalFactsApi {

    private final LegalFactService legalFactService;
    private final NotificationService notificationService;
    
    public PnLegalFactsController(LegalFactService legalFactService, NotificationService notificationService) { this.legalFactService = legalFactService;
        this.notificationService = notificationService;
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
            ServerWebExchange exchange
    ) {
        return Mono.fromSupplier(()-> {
            NotificationInt notificationInt = notificationService.getNotificationByIun(iun);
            
            //TODO Da implementare quando disponibile safeStorage
            return ResponseEntity.ok(new LegalFactDownloadMetadataResponse());
        });
    }

    @Override
    public Mono<ResponseEntity<Flux<LegalFactListElement>>> getNotificationLegalFacts(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            String iun,
            List<String> xPagopaPnCxGroups,
            ServerWebExchange exchange
    ) {
        List<LegalFactListElement> legalFacts = legalFactService.getLegalFacts(iun);
        Flux<LegalFactListElement> fluxFacts = Flux.fromStream(legalFacts.stream().map(this::convert));
        return Mono.just(ResponseEntity.ok(fluxFacts));
    }
    
    private LegalFactListElement convert(LegalFactListElement element){
        LegalFactListElement legalFactListElement = new LegalFactListElement();
        
        LegalFactsId legalFactsId = getLegalFactsId(element);
        legalFactListElement.setLegalFactsId(legalFactsId);
        legalFactListElement.setIun(element.getIun());
        legalFactListElement.setTaxId(element.getTaxId());
        
        return legalFactListElement;
    }

    private LegalFactsId getLegalFactsId(LegalFactListElement element) {
        LegalFactsId legalFactsId = new LegalFactsId();
        legalFactsId.setKey(element.getLegalFactsId().getKey());
        LegalFactCategory category = element.getLegalFactsId().getCategory();
        
        legalFactsId.setCategory(category);
        
        return legalFactsId;
    }

    @GetMapping(PnDeliveryPushRestConstants.LEGAL_FACT_BY_ID)
    public ResponseEntity<Resource> getLegalFact(String iun, LegalFactType legalFactType, String legalfactId) {
        return legalFactService.getLegalfact( iun, legalFactType, legalfactId );
    }
}
