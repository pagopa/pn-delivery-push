package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetDocumentService {

    Mono<DocumentDownloadMetadataResponse> getDocumentMetadata(String iun,
                                                               DocumentCategory documentType,
                                                               String documentId,
                                                               String recipientId
    );

    Mono<DocumentDownloadMetadataResponse> getDocumentWebMetadata(String iun,
                                                                  DocumentCategory documentType,
                                                                  String documentId,
                                                                  String senderReceiverId,
                                                                  String mandateId,
                                                                  CxTypeAuthFleet cxType,
                                                                  List<String> cxGroups
    );
}
