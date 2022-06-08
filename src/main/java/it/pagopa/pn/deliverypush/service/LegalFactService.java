package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LegalFactService {

    List<LegalFactListElement> getLegalFacts(String iun);
    ResponseEntity<Resource> getLegalfact(String iun, LegalFactCategory legalFactType, String legalfactId);

    LegalFactDownloadMetadataResponse getLegalFactMetadata(String iun, LegalFactCategory legalFactType, String legalfactId);
}
