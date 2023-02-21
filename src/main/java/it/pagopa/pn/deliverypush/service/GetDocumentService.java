package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import reactor.core.publisher.Mono;

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
                                                               String mandateId
    );
}
