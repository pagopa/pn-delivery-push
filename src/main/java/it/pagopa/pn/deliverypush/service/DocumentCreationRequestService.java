package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;

public interface DocumentCreationRequestService {
    void addDocumentCreationRequest(String fileKey, String iun, Integer recIndex, DocumentCreationTypeInt documentType, String timelineId);
}
