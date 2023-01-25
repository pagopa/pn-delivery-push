package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.DocumentCreationRequestDao;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class DocumentCreationRequestServiceImpl implements DocumentCreationRequestService {
    private final DocumentCreationRequestDao dao;
    
    @Override
    public void addDocumentCreationRequest(String fileKey, String iun, Integer recIndex, DocumentCreationRequest.DocumentCreationType documentType) {
        DocumentCreationRequest request = DocumentCreationRequest.builder()
                .key(fileKey)
                .iun(iun)
                .recIndex(recIndex)
                .documentCreationType(documentType)
                .build();
        
        dao.addDocumentCreationRequest(request);
    }

    @Override
    public void addDocumentCreationRequest(String fileKey, String iun, DocumentCreationRequest.DocumentCreationType documentType) {
        DocumentCreationRequest request = DocumentCreationRequest.builder()
                .key(fileKey)
                .iun(iun)
                .documentCreationType(documentType)
                .build();

        dao.addDocumentCreationRequest(request);
    }

    @Override
    public Optional<DocumentCreationRequest> getDocumentCreationRequest(String fileKey) {
        return dao.getDocumentCreationRequest(fileKey);
    }
}
