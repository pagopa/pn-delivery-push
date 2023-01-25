package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;

import java.util.Optional;

public interface DocumentCreationRequestService {
    void addDocumentCreationRequest(String fileKey, String iun, Integer recIndex, DocumentCreationRequest.DocumentCreationType documentType);

    void addDocumentCreationRequest(String fileKey, String iun, DocumentCreationRequest.DocumentCreationType documentType);

    Optional<DocumentCreationRequest> getDocumentCreationRequest(String fileKey);
}
