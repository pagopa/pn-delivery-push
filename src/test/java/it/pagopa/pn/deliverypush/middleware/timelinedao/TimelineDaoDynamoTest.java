package it.pagopa.pn.deliverypush.middleware.timelinedao;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.ReceivedDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.util.StatusUtils;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimelineDaoDynamoTest {

    private TimelineDao dao;
    private TimelineEntityDao entityDao;
    
    @Mock
    private NotificationService notificationService;
    @Mock
    private StatusUtils statusUtils;

    @Mock
    private PnDeliveryClient client;

    @BeforeEach
    void setup() {
        ObjectMapper objMapper = new ObjectMapper();
        DtoToEntityTimelineMapper dto2Entity = new DtoToEntityTimelineMapper(objMapper);
        EntityToDtoTimelineMapper entity2dto = new EntityToDtoTimelineMapper(objMapper);
        entityDao = new TestMyTimelineEntityDao();

        dao = new TimelineDaoDynamo(entityDao, dto2Entity, entity2dto, client, notificationService, statusUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void addTimelineUpdateStatusFail() {
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
        
        Notification notification = getNotification(iun);

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);
        List<NotificationStatusHistoryElement> firstListReturn = new ArrayList<>();
        NotificationStatusHistoryElement element = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.DELIVERING)        
                .build();
        firstListReturn.add(element);

        NotificationStatusHistoryElement element2 = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .build();
        List<NotificationStatusHistoryElement> secondListReturn = new ArrayList<>(firstListReturn);
        secondListReturn.add(element2);

        Mockito.when(statusUtils.getStatusHistory(Mockito.any(), Mockito.anyInt(), Mockito.any() ))
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn);

        Mockito.when(statusUtils.getStatusHistory(Mockito.any(), Mockito.anyInt(), Mockito.any() ))
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn);
        
        Mockito.when(client.updateState(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(ResponseEntity.internalServerError().build());

        // WHEN
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
        Notification notification = getNotification(iun);

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);
        List<NotificationStatusHistoryElement> firstListReturn = new ArrayList<>();
        NotificationStatusHistoryElement element = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.DELIVERING)
                .build();
        firstListReturn.add(element);

        NotificationStatusHistoryElement element2 = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .build();
        List<NotificationStatusHistoryElement> secondListReturn = new ArrayList<>(firstListReturn);
        secondListReturn.add(element2);

        Mockito.when(statusUtils.getStatusHistory(Mockito.any(), Mockito.anyInt(), Mockito.any() ))
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn)
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn);


        ResponseEntity<Void> respEntity = ResponseEntity.ok(null);
        Mockito.when(client.updateState(Mockito.any(RequestUpdateStatusDto.class))).thenReturn( respEntity );

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
        Notification notification = getNotification(iun);

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);
        List<NotificationStatusHistoryElement> firstListReturn = new ArrayList<>();
        NotificationStatusHistoryElement element = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.DELIVERING)
                .build();
        firstListReturn.add(element);

        NotificationStatusHistoryElement element2 = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .build();
        List<NotificationStatusHistoryElement> secondListReturn = new ArrayList<>(firstListReturn);
        secondListReturn.add(element2);

        Mockito.when(statusUtils.getStatusHistory(Mockito.any(), Mockito.anyInt(), Mockito.any() ))
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn)
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn);

        ResponseEntity<Void> respEntity = ResponseEntity.ok(null);
        Mockito.when(client.updateState(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(respEntity);

        dao.addTimelineElement(row1);
        dao.addTimelineElement(row2);

        dao.deleteTimeline(iun);

        // THEN
        Assertions.assertTrue(dao.getTimeline(iun).isEmpty());
    }

    private static class TestMyTimelineEntityDao implements TimelineEntityDao {

        private final Map<Key, TimelineElementEntity> store = new ConcurrentHashMap<>();

        @Override
        public void put(TimelineElementEntity timelineElementEntity) {
            Key key = Key.builder()
                    .partitionValue(timelineElementEntity.getIun())
                    .sortValue(timelineElementEntity.getTimelineElementId())
                    .build();
            this.store.put(key, timelineElementEntity);
        }

        @Override
        public void putIfAbsent(TimelineElementEntity timelineElementEntity) throws IdConflictException {
            Key key = Key.builder()
                    .partitionValue(timelineElementEntity.getIun())
                    .sortValue(timelineElementEntity.getTimelineElementId())
                    .build();

            if (this.store.put(key, timelineElementEntity) != null) {
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

    private Notification getNotification(String iun) {
        return Notification.builder()
                .iun(iun)
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }
}