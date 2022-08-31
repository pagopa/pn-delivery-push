package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

class StatusUtilsTest {
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private StatusUtils statusUtils;
    
    @BeforeEach
    public void setup() {
        Mockito.when(pnDeliveryPushConfigs.getPaperMessageNotHandled()).thenReturn(false);
        this.statusUtils = new StatusUtils(pnDeliveryPushConfigs);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getTimelineHistoryTest() {

        // GIVEN a timeline
        TimelineElementInternal timelineElement1 = TimelineElementInternal.builder()
                .elementId("el1")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal timelineElement3 = TimelineElementInternal.builder()
                .elementId("el3")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal timelineElement4 = TimelineElementInternal.builder()
                .elementId("el4")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal timelineElement5 = TimelineElementInternal.builder()
                .elementId("el5")
                .timestamp((Instant.parse("2021-09-16T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal timelineElement6 = TimelineElementInternal.builder()
                .elementId("el6")
                .timestamp((Instant.parse("2021-09-16T17:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal timelineElement7 = TimelineElementInternal.builder()
                .elementId("el7")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(timelineElement1, timelineElement3,
                timelineElement4, timelineElement5, timelineElement6, timelineElement7);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");
        
        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList, 1,
                notificationCreatedAt
        );

        // THEN status histories have same length
        Assertions.assertEquals( 6, actualStatusHistory.size(), "Check length");

        //  ... 1st initial status
        Assertions.assertEquals( NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.IN_VALIDATION)
                        .activeFrom(notificationCreatedAt)
                        .relatedTimelineElements(List.of())
                        .build(),
                actualStatusHistory.get( 0 ),
                "1st status wrong"
        );

        //  ... 2nd initial status
        Assertions.assertEquals( NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom( timelineElement1.getTimestamp() )
                        .relatedTimelineElements(List.of("el1"))
                        .build(),
                actualStatusHistory.get( 1 ),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals( NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom( timelineElement3.getTimestamp() )
                        .relatedTimelineElements( Arrays.asList(  "el3", "el4" ))
                        .build(),
                actualStatusHistory.get( 2 ),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals( NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom( timelineElement5.getTimestamp() )
                        .relatedTimelineElements(List.of("el5"))
                        .build(),
                actualStatusHistory.get( 3 ),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals( NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom( timelineElement6.getTimestamp() )
                        .relatedTimelineElements(List.of("el6"))
                        .build(),
                actualStatusHistory.get( 4 ),
                "2nd status wrong"
        );

        //  ... 6th initial status
        Assertions.assertEquals( NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.PAID)
                        .activeFrom( timelineElement7.getTimestamp() )
                        .relatedTimelineElements(List.of("el7"))
                        .build(),
                actualStatusHistory.get( 5 ),
                "6th status wrong"
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getTimelineHistoryMoreRecipientTest() {
        // GIVEN a timeline
        TimelineElementInternal timelineElement1 = TimelineElementInternal.builder()
                .elementId("el1")
                .timestamp((Instant.parse("2021-09-16T15:24:00.00Z")))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal timelineElement3 = TimelineElementInternal.builder()
                .elementId("el3")
                .timestamp((Instant.parse("2021-09-16T15:25:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal timelineElement4 = TimelineElementInternal.builder()
                .elementId("el4")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal timelineElement5 = TimelineElementInternal.builder()
                .elementId("el5")
                .timestamp((Instant.parse("2021-09-16T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .build();
        TimelineElementInternal timelineElement3_1 = TimelineElementInternal.builder()
                .elementId("el6")
                .timestamp((Instant.parse("2021-09-16T15:29:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal timelineElement4_1 = TimelineElementInternal.builder()
                .elementId("el7")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal timelineElement5_1 = TimelineElementInternal.builder()
                .elementId("el8")
                .timestamp((Instant.parse("2021-09-16T15:31:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal timelineElement6 = TimelineElementInternal.builder()
                .elementId("el9")
                .timestamp((Instant.parse("2021-09-16T17:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal timelineElement7 = TimelineElementInternal.builder()
                .elementId("el10")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(timelineElement1,
                timelineElement3, timelineElement4, timelineElement5, timelineElement3_1, timelineElement4_1,
                timelineElement5_1, timelineElement6, timelineElement7);


        // creare List<NotificationStatusHistoryElementInt>
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");
        
        NotificationStatusHistoryElementInt historyElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(notificationCreatedAt)
                .relatedTimelineElements(List.of())
                .build();

        NotificationStatusHistoryElementInt historyElement1 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom((Instant.parse("2021-09-16T15:24:00.00Z")))
                .relatedTimelineElements(List.of("el1"))
                .build();

        NotificationStatusHistoryElementInt historyElement2 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom((Instant.parse("2021-09-16T15:25:00.00Z")))
                .relatedTimelineElements( Arrays.asList( "el3", "el4", "el5", "el6", "el7" ))
                .build();
        NotificationStatusHistoryElementInt historyElement4_1 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERED)
                .activeFrom((Instant.parse("2021-09-16T15:31:00.00Z")))
                .relatedTimelineElements(List.of("el8"))
                .build();
        NotificationStatusHistoryElementInt historyElement5 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.VIEWED)
                .activeFrom((Instant.parse("2021-09-16T17:00:00.00Z")))
                .relatedTimelineElements(List.of("el9"))
                .build();
        NotificationStatusHistoryElementInt historyElement6 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.PAID)
                .activeFrom((Instant.parse("2021-09-16T17:30:00.00Z")))
                .relatedTimelineElements(List.of("el10"))
                .build();
        List<NotificationStatusHistoryElementInt> historyElementList = Arrays.asList(historyElement,historyElement1,
                historyElement2, historyElement4_1, historyElement5, historyElement6);

        // chiamare metodo di test
        List<NotificationStatusHistoryElementInt> resHistoryElementList = statusUtils.getStatusHistory(
                timelineElementList, 1,
                notificationCreatedAt
        );
        // verificare che è risultato atteso
        Assertions.assertEquals(historyElementList, resHistoryElementList);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getTimelineHistoryErrorTest() {
        // creare TimelineElement
        TimelineElementInternal timelineElement1 = TimelineElementInternal.builder()
                .elementId("el1")
                .timestamp((Instant.parse("2021-09-16T15:24:00.00Z")))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal timelineElement2 = TimelineElementInternal.builder()
                .elementId("el2")
                .timestamp((Instant.parse("2021-09-16T15:25:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal timelineElement3 = TimelineElementInternal.builder()
                .elementId("el3")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(timelineElement1,
                timelineElement2, timelineElement3);

        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:23:00.00Z");
        
        NotificationStatusHistoryElementInt historyElement1 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .relatedTimelineElements(List.of())
                .activeFrom(notificationCreatedAt)
                .build();

        NotificationStatusHistoryElementInt historyElement2 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom((Instant.parse("2021-09-16T15:24:00.00Z")))
                .relatedTimelineElements(List.of("el1"))
                .build();

        NotificationStatusHistoryElementInt historyElement3 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.VIEWED)
                .activeFrom((Instant.parse("2021-09-16T15:25:00.00Z")))
                .relatedTimelineElements(List.of("el2"))
                .build();

        NotificationStatusHistoryElementInt historyElement4 = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.PAID)
                .activeFrom((Instant.parse("2021-09-16T15:26:00.00Z")))
                .relatedTimelineElements(List.of("el3"))
                .build();

        List<NotificationStatusHistoryElementInt> historyElementList = Arrays.asList(
                historyElement1, historyElement2, historyElement3, historyElement4
        );

        // chiamare metodo di test
        List<NotificationStatusHistoryElementInt> resHistoryElementList = statusUtils.getStatusHistory(
                timelineElementList, 2,
                notificationCreatedAt
        );
        // verificare che è risultato atteso
        Assertions.assertEquals(historyElementList, resHistoryElementList);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void emptyTimelineInitialStateTest() {
        //
        Assertions.assertEquals(NotificationStatusInt.IN_VALIDATION, statusUtils.getCurrentStatus(Collections.emptyList()));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getCurrentStatusTest() {
        List<NotificationStatusHistoryElementInt>  statusHistory = new ArrayList<>();
        NotificationStatusHistoryElementInt statusHistoryDelivering = NotificationStatusHistoryElementInt.builder()
                .activeFrom(Instant.now())
                .status(NotificationStatusInt.DELIVERING)
                .build();
        NotificationStatusHistoryElementInt statusHistoryAccepted = NotificationStatusHistoryElementInt.builder()
                .activeFrom(Instant.now())
                .status(NotificationStatusInt.ACCEPTED)
                .build();
        statusHistory.add(statusHistoryDelivering);
        statusHistory.add(statusHistoryAccepted);

        Assertions.assertEquals(NotificationStatusInt.ACCEPTED, statusUtils.getCurrentStatus(statusHistory));
    }

    @Test
    void getStatusHistory() {
    }
}