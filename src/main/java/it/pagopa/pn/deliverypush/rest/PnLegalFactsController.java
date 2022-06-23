package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.LegalFactsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class PnLegalFactsController implements LegalFactsApi {

    private final GetLegalFactService getLegalFactService;

    public PnLegalFactsController(GetLegalFactService getLegalFactService) { this.getLegalFactService = getLegalFactService; }

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
        return Mono.fromSupplier(() -> ResponseEntity.ok(getLegalFactService.getLegalFactMetadata(iun, legalFactType, legalFactId)));
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
        return Mono.fromSupplier(() -> {
             List<LegalFactListElement> legalFacts = getLegalFactService.getLegalFacts(iun);
             Flux<LegalFactListElement> fluxFacts = Flux.fromStream(legalFacts.stream().map(this::convert));
             return ResponseEntity.ok(fluxFacts);
         });
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
    public ResponseEntity<Resource> getLegalFact(@PathVariable(value="iun") String iun,
                                                 @PathVariable(value="type") LegalFactCategory legalFactType,
                                                 @PathVariable(value="id") String legalfactId) {
        return getLegalFactService.getLegalfact( iun, legalFactType, legalfactId );
    }
}
