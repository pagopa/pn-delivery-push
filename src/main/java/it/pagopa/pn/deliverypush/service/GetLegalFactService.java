package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;

import java.util.List;

public interface GetLegalFactService {

    LegalFactDownloadMetadataResponse getLegalFactMetadata(String iun, LegalFactCategory legalFactType, String legalfactId, String senderReceiverId, String mandateId,
                                                           CxTypeAuthFleet cxType, List<String> cxGroups);

    List<LegalFactListElement> getLegalFacts(String iun, String senderReceiverId, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups);
}
