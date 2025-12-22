package it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper.DtoToEntityDocumentCreationRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class DocumentCreationRequestDaoDynamoTest {
    @Mock
    private DocumentCreationRequestEntityDao documentCreationRequestEntityDao;
    
    private DocumentCreationRequestDao documentCreationRequestDao;
    private DtoToEntityDocumentCreationRequestMapper dtoToEntity;

    @BeforeEach
    void setup() {
        dtoToEntity = new DtoToEntityDocumentCreationRequestMapper();
        documentCreationRequestDao = new DocumentCreationRequestDaoDynamo(documentCreationRequestEntityDao, dtoToEntity);
    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void addDocumentCreationRequest() {
        //GIVEN
        DocumentCreationRequest request = DocumentCreationRequest.builder()
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .key("key")
                .recIndex(0)
                .timelineId("timelineId")
                .iun("iun")
                .build();
        
        //WHEN
        documentCreationRequestDao.addDocumentCreationRequest(request);
        
        //THEN
        Mockito.verify(documentCreationRequestEntityDao).put(dtoToEntity.dto2Entity(request));
    }

}