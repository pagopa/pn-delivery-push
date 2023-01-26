package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;

import java.util.Optional;

public interface DocumentCreationRequestService {
    void addDocumentCreationRequest(String fileKey, String iun, Integer recIndex, DocumentCreationTypeInt documentType, String timelineId);

    void addDocumentCreationRequest(String fileKey, String iun, DocumentCreationTypeInt documentType, String timelineId);

    Optional<DocumentCreationRequest> getDocumentCreationRequest(String fileKey);
}
