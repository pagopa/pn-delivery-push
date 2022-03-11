package it.pagopa.pn.deliverypush.middleware.timelinedao;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.ReceivedDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.middleware.model.notification.TimelineElementEntity;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimelineDaoDynamoTest {
    
    private TimelineDao dao;

    @Mock
    private PnDeliveryClient client;
    
    @BeforeEach
    void setup(){
        ObjectMapper objMapper = new ObjectMapper();
        DtoToEntityTimelineMapper dto2Entity = new DtoToEntityTimelineMapper(objMapper);
        EntityToDtoTimelineMapper entity2dto = new EntityToDtoTimelineMapper(objMapper);
        TimelineEntityDao<TimelineElementEntity,Key> entityDao = new TestMyTimelineEntityDao();

        dao = new TimelineDaoDynamo(entityDao, dto2Entity, entity2dto,  client);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void addTimelineUpdateStatusFail(){
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        String id1 = "sender_ack";
        TimelineElement row1 = TimelineElement.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(new ReceivedDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();

        // WHEN
        Mockito.when(client.updateState(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(ResponseEntity.internalServerError().build());

        assertThrows(PnInternalException.class, () -> {
            dao.addTimelineElement(row1);
        });
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void successfullyInsertAndRetrieve() {
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        String id1 = "sender_ack";
        TimelineElement row1 = TimelineElement.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(new ReceivedDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();
        String id2 = "path_choose";
        TimelineElement row2 = TimelineElement.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategory.NOTIFICATION_PATH_CHOOSE)
                .details(new NotificationPathChooseDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();

        // WHEN
        ResponseUpdateStatusDto dto = ResponseUpdateStatusDto.builder().build();
        ResponseEntity<ResponseUpdateStatusDto> respEntity = ResponseEntity.ok(dto);
        Mockito.when(client.updateState(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(respEntity);
        
        dao.addTimelineElement(row1);
        dao.addTimelineElement(row2);
        
        // THEN
        // check first row
        Optional<TimelineElement> retrievedRow1 = dao.getTimelineElement(iun, id1);
        Assertions.assertTrue(retrievedRow1.isPresent());
        Assertions.assertEquals(row1, retrievedRow1.get());

        // check second row
        Optional<TimelineElement> retrievedRow2 = dao.getTimelineElement(iun, id2);
        Assertions.assertTrue(retrievedRow2.isPresent());
        Assertions.assertEquals(row2, retrievedRow2.get());

        // check full retrieve
        Set<TimelineElement> result = dao.getTimeline(iun);
        Assertions.assertEquals(Set.of(row1, row2), result);        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void successfullyDelete() {
        // GIVEN
        String iun = "iun1";

        String id1 = "sender_ack";
        TimelineElement row1 = TimelineElement.builder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(new ReceivedDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();
        String id2 = "path_choose";
        TimelineElement row2 = TimelineElement.builder()
                .iun(iun)
                .elementId(id2)
                .category(TimelineElementCategory.NOTIFICATION_PATH_CHOOSE)
                .details(new NotificationPathChooseDetails())
                .timestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
                .build();

        // WHEN
        ResponseUpdateStatusDto dto = ResponseUpdateStatusDto.builder().build();
        ResponseEntity<ResponseUpdateStatusDto> respEntity = ResponseEntity.ok(dto);
        Mockito.when(client.updateState(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(respEntity);

        dao.addTimelineElement(row1);
        dao.addTimelineElement(row2);
        
        dao.deleteTimeline(iun);

        // THEN
        Assertions.assertTrue(dao.getTimeline(iun).isEmpty());
    }

    private static class TestMyTimelineEntityDao implements TimelineEntityDao<TimelineElementEntity, Key> {

        private final Map<Key,TimelineElementEntity> store = new ConcurrentHashMap<>();

        @Override
        public void put(TimelineElementEntity timelineElementEntity) {
            Key key = Key.builder()
                    .partitionValue(timelineElementEntity.getIun())
                    .sortValue(timelineElementEntity.getTimelineElementId())
                    .build();
            this.store.put(key, timelineElementEntity);
        }

        @Override
        public void putIfAbsent(TimelineElementEntity timelineElementEntity, Key key) throws IdConflictException {
            if (this.store.putIfAbsent(key, timelineElementEntity) != null) {
                throw new IdConflictException(key);
            }
        }

        @Override
        public Optional<TimelineElementEntity> get(Key key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void delete(Key key) {
            store.remove(key);
        }


        @Override
        public Set<TimelineElementEntity> findByIun(String iun) {
            return this.store.values().stream()
                    .filter(el -> iun.equals(el.getIun()))
                    .collect(Collectors.toSet());
        }

        @Override
        public void deleteByIun(String iun) {
            Set<Key> toRemove = this.store.keySet().stream()
                    .filter(key -> iun.equals(key.partitionKeyValue().s()))
                    .collect(Collectors.toSet());

            for (Key key : toRemove) {
                this.store.remove(key);
            }
        }

    }

}