package it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.entity.DocumentCreationRequestEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DtoToEntityDocumentCreationRequestMapperTest {
    private DtoToEntityDocumentCreationRequestMapper mapper;

    @BeforeEach
    void before() {
        mapper = new DtoToEntityDocumentCreationRequestMapper();
    }
    
    @Test
    void dto2Entity() {

        DocumentCreationRequest request = DocumentCreationRequest.builder()
                .iun("iun")
                .timelineId("timelineId")
                .recIndex(1)
                .key("keyY")
                .documentCreationType(DocumentCreationTypeInt.SENDER_ACK)
                .build();

        DocumentCreationRequestEntity entity = mapper.dto2Entity(request);

        Assertions.assertEquals(request.getIun(), entity.getIun());
        Assertions.assertEquals(request.getKey(), entity.getKey());
        Assertions.assertEquals(request.getTimelineId(), entity.getTimelineId());
        Assertions.assertEquals(request.getDocumentCreationType(), DocumentCreationTypeInt.valueOf(entity.getDocumentType()) );
        Assertions.assertEquals(request.getRecIndex(), entity.getRecIndex());
    }
}