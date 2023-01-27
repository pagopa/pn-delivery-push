package it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.entity.DocumentCreationRequestEntity;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper.DtoToEntityDocumentCreationRequestMapper;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper.EntityToDtoDocumentCreationRequestMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

class DocumentCreationRequestDaoDynamoTest {
    @Mock
    private DocumentCreationRequestEntityDao documentCreationRequestEntityDao;
    
    private DocumentCreationRequestDao documentCreationRequestDao;
    private DtoToEntityDocumentCreationRequestMapper dtoToEntity;
    private EntityToDtoDocumentCreationRequestMapper entityToDto;
    
    @BeforeEach
    void setup() {
        dtoToEntity = new DtoToEntityDocumentCreationRequestMapper();
        entityToDto = new EntityToDtoDocumentCreationRequestMapper();
        documentCreationRequestDao = new DocumentCreationRequestDaoDynamo(documentCreationRequestEntityDao, dtoToEntity, entityToDto);
    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void addDocumentCreationRequest() {
        //GIVEN
        DocumentCreationRequest request = DocumentCreationRequest.builder()
                .documentCreationType(DocumentCreationTypeInt.SENDER_ACK)
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

    @ExtendWith(SpringExtension.class)
    @Test
    void getDocumentCreationRequest() {
        //GIVEN
        String keyToSearch = "key";
        DocumentCreationRequestEntity entity = DocumentCreationRequestEntity.builder()
                .documentType(DocumentCreationTypeInt.SENDER_ACK.getValue())
                .key("key")
                .recIndex(0)
                .timelineId("timelineId")
                .iun("iun")
                .build();
        
        Mockito.when(documentCreationRequestEntityDao.get(Mockito.any())).thenReturn(Optional.of(entity));
        
        //WHEN
        Optional<DocumentCreationRequest> requestOpt = documentCreationRequestDao.getDocumentCreationRequest(keyToSearch);

        //THEN
        Assertions.assertTrue(requestOpt.isPresent());
        DocumentCreationRequest request = requestOpt.get();
        Assertions.assertEquals(entity, dtoToEntity.dto2Entity(request));
    }
}