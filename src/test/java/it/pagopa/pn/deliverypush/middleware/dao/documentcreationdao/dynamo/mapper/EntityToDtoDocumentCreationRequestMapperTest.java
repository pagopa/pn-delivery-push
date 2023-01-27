package it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.entity.DocumentCreationRequestEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityToDtoDocumentCreationRequestMapperTest {
    private EntityToDtoDocumentCreationRequestMapper mapper;

    @BeforeEach
    void before() {
        mapper = new EntityToDtoDocumentCreationRequestMapper();
    }
    
    @Test
    void entityToDto() {
        DocumentCreationRequestEntity entity = DocumentCreationRequestEntity.builder()
                .iun("iun")
                .timelineId("timelineId")
                .recIndex(1)
                .key("keyY")
                .documentType(DocumentCreationTypeInt.SENDER_ACK.getValue())
                .build();

        DocumentCreationRequest request = mapper.entityToDto(entity);

        Assertions.assertEquals(entity.getIun(), request.getIun());
        Assertions.assertEquals(entity.getKey(), request.getKey());
        Assertions.assertEquals(entity.getTimelineId(), request.getTimelineId());
        Assertions.assertEquals(DocumentCreationTypeInt.valueOf(entity.getDocumentType()), request.getDocumentCreationType() );
        Assertions.assertEquals(entity.getRecIndex(), request.getRecIndex());

    }
}