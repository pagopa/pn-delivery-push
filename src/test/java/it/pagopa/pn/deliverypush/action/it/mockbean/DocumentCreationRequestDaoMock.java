package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.DocumentCreationRequestDao;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class DocumentCreationRequestDaoMock implements DocumentCreationRequestDao {
    private ConcurrentMap<String, DocumentCreationRequest> documentMap;

    public void clear() {
        this.documentMap = new ConcurrentHashMap<>();
    }

    @Override
    public void addDocumentCreationRequest(DocumentCreationRequest documentCreationRequest) {
        documentMap.put(documentCreationRequest.getKey(), documentCreationRequest);
    }

    @Override
    public Optional<DocumentCreationRequest> getDocumentCreationRequest(String key) {
        DocumentCreationRequest documentCreationRequest = documentMap.get(key);
        return documentCreationRequest != null ? Optional.of(documentCreationRequest) : Optional.empty();
    }
}
