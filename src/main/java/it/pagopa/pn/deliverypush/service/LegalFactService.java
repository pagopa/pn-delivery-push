package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LegalFactService {

    List<LegalFactsListEntry> getLegalFacts(String iun);
    ResponseEntity<Resource> getLegalfact(String iun, LegalFactType type, String legalfactId );
}
