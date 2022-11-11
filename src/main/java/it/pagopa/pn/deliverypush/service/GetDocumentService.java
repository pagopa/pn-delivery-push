package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;

public interface GetDocumentService {
    DocumentDownloadMetadataResponse getDocumentMetadata(String iun,
                                                          DocumentCategory documentType,
                                                          String documentId,
                                                          String recipientId
    );
}
