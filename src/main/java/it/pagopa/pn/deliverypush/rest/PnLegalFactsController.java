package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.rest.PnDeliveryPushRestApi_methodGetLegalFacts;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PnLegalFactsController implements PnDeliveryPushRestApi_methodGetLegalFacts {

    private LegalFactService legalFactService;

    public PnLegalFactsController(LegalFactService legalFactService) { this.legalFactService = legalFactService; }

    @Override
    @GetMapping(PnDeliveryPushRestConstants.LEGAL_FACTS_BY_IUN)
    public List<LegalFactsListEntry> getLegalFacts(String iun) {
        return legalFactService.getLegalFacts( iun );
    }

    @Override
    @GetMapping(PnDeliveryPushRestConstants.LEGAL_FACT_BY_ID)
    public ResponseEntity<Resource> getLegalFact(String iun, LegalFactType legalFactType, String legalfactId) {
        return legalFactService.getLegalfact( iun, legalFactType, legalfactId );
    }
}
