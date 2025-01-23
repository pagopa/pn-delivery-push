package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestDao;
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
        if(documentMap.get(documentCreationRequest.getKey()) != null){
            log.error("[TEST] Cannot save more than one addDocumentCreationRequest with same fileKey {}",documentCreationRequest.getKey());
            throw new RuntimeException("Cannot save more than one addDocumentCreationRequest with same fileKey");
        }
        documentMap.put(documentCreationRequest.getKey(), documentCreationRequest);
        log.info("document added to documentMap {}", documentCreationRequest);
    }

    @Override
    public Optional<DocumentCreationRequest> getDocumentCreationRequest(String key) {
        DocumentCreationRequest documentCreationRequest = documentMap.get(key);
        
        Optional<DocumentCreationRequest> documentCreationRequestOptional = documentCreationRequest != null ? Optional.of(documentCreationRequest) : Optional.empty();
        if(documentCreationRequestOptional.isPresent()){
            return documentCreationRequestOptional;
        }else {
            log.info("document creation request for fileKey {} is not present. This is documentCreationRequestPresent={}", key, documentMap.toString());
            return Optional.empty();
        }
    }
}
