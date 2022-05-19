package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineElementEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineEntityDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566",
})
@SpringBootTest
class TimelineEntityDaoDynamoTestIT {
    @Autowired
    private TimelineEntityDao timelineEntityDao;
    
    @Test
    void put() {
        //GIVEN
        TimelineElementEntity elementToInsert = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE.getValue())
                .details("{\"category\":\"SEND_DIGITAL_DOMICILE\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        removeElementFromDb(elementToInsert);

        //WHEN
        timelineEntityDao.put(elementToInsert);
        
        //THEN
        Key key = Key.builder()
                .partitionValue(elementToInsert.getIun())
                .sortValue(elementToInsert.getTimelineElementId())
                .build();

        Optional<TimelineElementEntity> elementFromDbOpt =  timelineEntityDao.get(key);

        Assertions.assertTrue(elementFromDbOpt.isPresent());
        TimelineElementEntity elementFromDb = elementFromDbOpt.get();
        Assertions.assertEquals(elementToInsert, elementFromDb);
    }

    @Test
    void putIfAbsentKo() {
        
        //GIVEN
        TimelineElementEntity elementToInsert = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL.getValue())
                .details("{\"category\":\"PUBLIC_REGISTRY_CALL\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        TimelineElementEntity elementNotToBeInserted = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue())
                .details("{\"category\":\"END_OF_DIGITAL_DELIVERY_WORKFLOW\",\"taxId\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        Key elementsKey = Key.builder()
                .partitionValue(elementToInsert.getIun())
                .sortValue(elementToInsert.getTimelineElementId())
                .build();

        removeElementFromDb(elementToInsert);
        removeElementFromDb(elementNotToBeInserted);

        
        assertDoesNotThrow(() -> timelineEntityDao.putIfAbsent(elementToInsert));

        //WHEN
        
        assertThrows(IdConflictException.class, () -> {
            timelineEntityDao.putIfAbsent(elementNotToBeInserted);
        });
        
        //THEN
        Optional<TimelineElementEntity> elementFromDbOpt =  timelineEntityDao.get(elementsKey);

        Assertions.assertTrue(elementFromDbOpt.isPresent());
        TimelineElementEntity elementFromDb = elementFromDbOpt.get();
        Assertions.assertEquals(elementToInsert, elementFromDb);
        Assertions.assertNotEquals(elementNotToBeInserted, elementFromDb);
    }

    @Test
    void putIfAbsentOk() {

        //GIVEN
        TimelineElementEntity firstElementToInsert = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.NOTIFICATION_VIEWED.getValue())
                .details("{\"category\":\"NOTIFICATION_VIEWED\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        Key firstElementsKey = Key.builder()
                .partitionValue(firstElementToInsert.getIun())
                .sortValue(firstElementToInsert.getTimelineElementId())
                .build();

        TimelineElementEntity secondElementToInsert = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId2")
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue())
                .details("{\"category\":\"END_OF_DIGITAL_DELIVERY_WORKFLOW\",\"taxId\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        Key secondElementsKey = Key.builder()
                .partitionValue(secondElementToInsert.getIun())
                .sortValue(secondElementToInsert.getTimelineElementId())
                .build();
        
        removeElementFromDb(firstElementToInsert);
        removeElementFromDb(secondElementToInsert);

        //WHEN
        assertDoesNotThrow(() -> timelineEntityDao.putIfAbsent(firstElementToInsert));
        assertDoesNotThrow(() -> timelineEntityDao.putIfAbsent(secondElementToInsert));

        //THEN
        Optional<TimelineElementEntity> firstElementFromDbOpt =  timelineEntityDao.get(firstElementsKey);
        Assertions.assertTrue(firstElementFromDbOpt.isPresent());
        TimelineElementEntity firstElementFromDb = firstElementFromDbOpt.get();
        Assertions.assertEquals(firstElementToInsert, firstElementFromDb);

        Optional<TimelineElementEntity> secondElementFromDbOpt =  timelineEntityDao.get(secondElementsKey);
        Assertions.assertTrue(secondElementFromDbOpt.isPresent());
        TimelineElementEntity secondElementFromDb = secondElementFromDbOpt.get();
        Assertions.assertEquals(secondElementToInsert, secondElementFromDb);
    }
    
    @Test
    void get() {

        //GIVEN
        TimelineElementEntity firstElementToInsert = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.REQUEST_ACCEPTED.getValue())
                .details("{\"category\":\"REQUEST_ACCEPTED\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();
        Key firstElementToInsertKey = Key.builder()
                .partitionValue(firstElementToInsert.getIun())
                .sortValue(firstElementToInsert.getTimelineElementId())
                .build();

        TimelineElementEntity secondElementToInsert = TimelineElementEntity.builder()
                .iun("pa1-2")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue())
                .details("{\"category\":\"SEND_ANALOG_DOMICILE\",\"taxId\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();
        Key secondElementToInsertKey = Key.builder()
                .partitionValue(secondElementToInsert.getIun())
                .sortValue(secondElementToInsert.getTimelineElementId())
                .build();
        
        removeElementFromDb(firstElementToInsert);
        timelineEntityDao.put(firstElementToInsert);

        removeElementFromDb(secondElementToInsert);
        timelineEntityDao.put(secondElementToInsert);
        
        //Check first element
        //WHEN
        Optional<TimelineElementEntity> firstElementFromDbOpt =  timelineEntityDao.get(firstElementToInsertKey);
        
        //THEN
        Assertions.assertTrue(firstElementFromDbOpt.isPresent());
        TimelineElementEntity firstElementFromDb = firstElementFromDbOpt.get();
        Assertions.assertEquals(firstElementToInsert, firstElementFromDb);

        //Check second element
        //WHEN
        Optional<TimelineElementEntity> secondElementFromDbOpt =  timelineEntityDao.get(secondElementToInsertKey);

        //THEN
        Assertions.assertTrue(secondElementFromDbOpt.isPresent());
        TimelineElementEntity secondElementFromDb = secondElementFromDbOpt.get();
        Assertions.assertEquals(secondElementToInsert, secondElementFromDb);
    }

    @Test
    void getNoElement() {

        //GIVEN
        TimelineElementEntity element = TimelineElementEntity.builder()
                .iun("pa1-1")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE.getValue())
                .details("{\"category\":\"SEND_DIGITAL_DOMICILE\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();
        Key elementToInsertKey = Key.builder()
                .partitionValue(element.getIun())
                .sortValue(element.getTimelineElementId())
                .build();
        
        removeElementFromDb(element);

        //Check first element
        //WHEN
        Optional<TimelineElementEntity> firstElementFromDbOpt =  timelineEntityDao.get(elementToInsertKey);

        //THEN
        Assertions.assertTrue(firstElementFromDbOpt.isEmpty());
    }
    
    @Test
    void delete() {
        //GIVEN
        TimelineElementEntity elementToInsert = TimelineElementEntity.builder()
                .iun("pa1-delete")
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL.getValue())
                .details("{\"category\":\"PUBLIC_REGISTRY_CALL\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        removeElementFromDb(elementToInsert);
        timelineEntityDao.put(elementToInsert);
        
        //WHEN
        Key key = Key.builder()
                .partitionValue(elementToInsert.getIun())
                .sortValue(elementToInsert.getTimelineElementId())
                .build();

        timelineEntityDao.delete(key);
        
        //THEN
        Optional<TimelineElementEntity> elementFromDbOpt =  timelineEntityDao.get(key);

        Assertions.assertTrue(elementFromDbOpt.isEmpty());
    }
    
    @Test
    void findByIun() {
        String iun = "pa1-1";
        
        //GIVEN
        TimelineElementEntity firstElementToInsert = TimelineElementEntity.builder()
                .iun(iun)
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.REFINEMENT.getValue())
                .details("{\"category\":\"REFINEMENT\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        TimelineElementEntity secondElementToInsert = TimelineElementEntity.builder()
                .iun(iun)
                .timelineElementId("elementId2")
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue())
                .details("{\"category\":\"SEND_ANALOG_DOMICILE\",\"taxId\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        removeElementFromDb(firstElementToInsert);
        timelineEntityDao.put(firstElementToInsert);
        removeElementFromDb(secondElementToInsert);
        timelineEntityDao.put(secondElementToInsert);

        //WHEN
        Set<TimelineElementEntity> elementSet =  timelineEntityDao.findByIun(iun);

        //THEN
        Assertions.assertFalse(elementSet.isEmpty());
        Assertions.assertTrue(elementSet.contains(firstElementToInsert));
        Assertions.assertTrue(elementSet.contains(secondElementToInsert));
    }

    @Test
    void findByIunNoElements() {
        String iun = "pa1-1";
        timelineEntityDao.deleteByIun(iun);

        //WHEN
        Set<TimelineElementEntity> elementSet =  timelineEntityDao.findByIun(iun);

        //THEN
        Assertions.assertTrue(elementSet.isEmpty());
    }

    @Test
    void deleteByIun() {
        String iun = "pa1-1";

        //GIVEN
        TimelineElementEntity firstElementToInsert = TimelineElementEntity.builder()
                .iun(iun)
                .timelineElementId("elementId1")
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL.getValue())
                .details("{\"category\":\"PUBLIC_REGISTRY_CALL\",\"iun\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        TimelineElementEntity secondElementToInsert = TimelineElementEntity.builder()
                .iun(iun)
                .timelineElementId("elementId2")
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue())
                .details("{\"category\":\"SEND_ANALOG_DOMICILE\",\"taxId\":\"ed84b8c9-444e-410d-80d7-cfad6aa12070\"}")
                .legalFactId("[{\"key\":\"key\",\"type\":\"DIGITAL_DELIVERY\"}]")
                .build();

        removeElementFromDb(firstElementToInsert);
        timelineEntityDao.put(firstElementToInsert);
        removeElementFromDb(secondElementToInsert);
        timelineEntityDao.put(secondElementToInsert);

        //Check elements is present
        Set<TimelineElementEntity> elementSet =  timelineEntityDao.findByIun(iun);
        Assertions.assertFalse(elementSet.isEmpty());
        Assertions.assertTrue(elementSet.contains(firstElementToInsert));
        Assertions.assertTrue(elementSet.contains(secondElementToInsert));
        
        //WHEN
        timelineEntityDao.deleteByIun(iun);

        //THEN
        //Check elements is not present
        Set<TimelineElementEntity> elementSetAfterDelete =  timelineEntityDao.findByIun(iun);
        Assertions.assertTrue(elementSetAfterDelete.isEmpty());

    }
    
    private void removeElementFromDb(TimelineElementEntity element) {
        Key key = Key.builder()
                .partitionValue(element.getIun())
                .sortValue(element.getTimelineElementId())
                .build();

        timelineEntityDao.delete(key);
    }

}