package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetLegalFactService {

    Mono<LegalFactDownloadMetadataResponse> getLegalFactMetadata(String iun, LegalFactCategory legalFactType, String legalfactId, String senderReceiverId, String mandateId,
                                                                 CxTypeAuthFleet cxType, List<String> cxGroups);

    Mono<LegalFactDownloadMetadataWithContentTypeResponse> getLegalFactMetadataWithContentType(String iun,
                                                                                               LegalFactCategory legalFactType,
                                                                                               String legalfactId,
                                                                                               String senderReceiverId,
                                                                                               String mandateId,
                                                                                               CxTypeAuthFleet cxType,
                                                                                               List<String> cxGroups);

    List<LegalFactListElement> getLegalFacts(String iun, String senderReceiverId, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups);
}
