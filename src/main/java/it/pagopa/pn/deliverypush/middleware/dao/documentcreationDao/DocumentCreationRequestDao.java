package it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;

import java.util.Optional;

public interface DocumentCreationRequestDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.document-creation";
    
    void addDocumentCreationRequest(DocumentCreationRequest documentCreationRequest);

    Optional<DocumentCreationRequest> getDocumentCreationRequest(String key);
}
