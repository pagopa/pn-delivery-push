package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

class StatusUtilsTest {

    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private StatusUtils statusUtils;

    @BeforeEach
    public void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        this.statusUtils = new StatusUtils(pnDeliveryPushConfigs);
    }

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
        Assertions.assertEquals(6, actualStatusHistory.size(), "Check length");

        //  ... 1st initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.IN_VALIDATION)
                        .activeFrom(notificationCreatedAt)
                        .relatedTimelineElements(List.of())
                        .build(),
                actualStatusHistory.get(0),
                "1st status wrong"
        );

        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(timelineElement1.getTimestamp())
                        .relatedTimelineElements(List.of("el1"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(timelineElement3.getTimestamp())
                        .relatedTimelineElements(Arrays.asList("el3", "el4"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(timelineElement5.getTimestamp())
                        .relatedTimelineElements(List.of("el5"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(timelineElement6.getTimestamp())
                        .relatedTimelineElements(List.of("el6"))
                        .build(),
                actualStatusHistory.get(4),
                "2nd status wrong"
        );

        //  ... 6th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.PAID)
                        .activeFrom(timelineElement7.getTimestamp())
                        .relatedTimelineElements(List.of("el7"))
                        .build(),
                actualStatusHistory.get(5),
                "6th status wrong"
        );
    }

    // IN_VALIDATION - ACCEPTED - DELIVERING - VIEWED - PAID
    // tutti e 3 destinatari sono raggiungibili via domicilio digitale ma un destinatario visualizza la notifica sul portale di PN
    // prima che la visualizzi su PEC, e poi la paga. Stato finale: PAID
    @Test
    void getTimelineHistoryMultiRecipientWithOneViewInPNTest() {
        final int NUMBER_OF_RECIPIENTS = 3;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:40.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        //uno dei 3 destinatari visualizza la notifica sul portale di PN
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        //tutti e 3 destinatari ricevono con successo la notifica via PEC
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //uno dei 3 destinatari visualizza la notifica dalla PEC
        TimelineElementInternal viewedFromPecTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPecTimelineElement")
                .timestamp((Instant.parse("2021-09-16T16:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        //uno dei 3 destinatari paga la multa
        TimelineElementInternal paymentTimelineElement = TimelineElementInternal.builder()
                .elementId("paymentTimelineElement")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                feedbackOKFirstRecipientTimelineElement, feedbackOKSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, viewedFromPNTimelineElement, pecReceivedFirstRecipientTimelineElement, pecReceivedSecondRecipientTimelineElement,
                pecReceivedThirdRecipientTimelineElement, viewedFromPecTimelineElement, paymentTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );


        // THEN status histories have 5 elements
        Assertions.assertEquals(5, actualStatusHistory.size(), "Check length");

//        //  ... 1st initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.IN_VALIDATION)
                        .activeFrom(notificationCreatedAt)
                        .relatedTimelineElements(List.of())
                        .build(),
                actualStatusHistory.get(0),
                "1st status wrong"
        );
//
//        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );
//
//        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "feedbackOKSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
//
//        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement","pecReceivedFirstRecipientTimelineElement", "pecReceivedSecondRecipientTimelineElement", "pecReceivedThirdRecipientTimelineElement", "viewedFromPecTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
//
//        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.PAID)
                        .activeFrom(paymentTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("paymentTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5th status wrong"
        );
    }

    // IN VALIDATION - ACCEPTED - DELIVERING - DELIVERED
    // 2 destinatari su 3 sono non raggiungibili, uno è raggiungibile, stato finale: DELIVERED
    @Test
    void getTimelineHistoryMultiRecipientWithOneSuccessTest() {
        final int NUMBER_OF_RECIPIENTS = 3;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:40.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal feedbackKOFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackKOSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        //1 destinatario riceve con successo la notifica via PEC, mentre 2 non sono raggiungibili
        TimelineElementInternal unreachableFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal unreachableSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal pecReceivedTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                feedbackKOFirstRecipientTimelineElement, feedbackKOSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, unreachableFirstRecipientTimelineElement, unreachableSecondRecipientTimelineElement, pecReceivedTimelineElement
                );


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );


        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

//        //  ... 1st initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.IN_VALIDATION)
                        .activeFrom(notificationCreatedAt)
                        .relatedTimelineElements(List.of())
                        .build(),
                actualStatusHistory.get(0),
                "1st status wrong"
        );
//
//        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );
//
//        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackKOFirstRecipientTimelineElement", "feedbackKOSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement", "unreachableFirstRecipientTimelineElement",
                                "unreachableSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
//
//
//        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(pecReceivedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("pecReceivedTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    // IN VALIDATION - ACCEPTED - DELIVERING - UNREACHABLE
    // tutti e 3 destinatari non sono raggiungibili e nessuno dei 3 visualizza la notifica su PN, stato finale: UNREACHABLE
    @Test
    void getTimelineHistoryMultiRecipientWithAllUnreachableTest() {
        final int NUMBER_OF_RECIPIENTS = 3;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:40.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal feedbackKOFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackKOSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackKOThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        //tutti i destinatari non sono raggiungibili
        TimelineElementInternal unreachableFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal unreachableSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal unreachableThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                feedbackKOFirstRecipientTimelineElement, feedbackKOSecondRecipientTimelineElement, feedbackKOThirdRecipientTimelineElement, unreachableFirstRecipientTimelineElement, unreachableSecondRecipientTimelineElement, unreachableThirdRecipientTimelineElement
        );


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );


        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

//        //  ... 1st initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.IN_VALIDATION)
                        .activeFrom(notificationCreatedAt)
                        .relatedTimelineElements(List.of())
                        .build(),
                actualStatusHistory.get(0),
                "1st status wrong"
        );
//
//        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );
//
//        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackKOFirstRecipientTimelineElement", "feedbackKOSecondRecipientTimelineElement",
                                "feedbackKOThirdRecipientTimelineElement", "unreachableFirstRecipientTimelineElement",
                                "unreachableSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
//
//
//        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.UNREACHABLE)
                        .activeFrom(unreachableThirdRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("unreachableThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    // IN VALIDATION - ACCEPTED - DELIVERING - UNREACHABLE - VIEWED
    // tutti e 3 destinatari non sono raggiungibili ma 1 dei 3 visualizza la notifica su PN, dopo che PN ha ricevuto i
    // feedback negativi da External Channels. Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientWithAllUnreachableButOneViewedAfterKOFeedbackFromPNTest() {
        final int NUMBER_OF_RECIPIENTS = 3;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:40.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal feedbackKOFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackKOSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackKOThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        //tutti i destinatari non sono raggiungibili
        TimelineElementInternal unreachableFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal unreachableSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal unreachableThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T16:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement,
                sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement, feedbackKOFirstRecipientTimelineElement,
                feedbackKOSecondRecipientTimelineElement, feedbackKOThirdRecipientTimelineElement, unreachableFirstRecipientTimelineElement,
                unreachableSecondRecipientTimelineElement, unreachableThirdRecipientTimelineElement, viewedFromPNTimelineElement
        );


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );


        // THEN status histories have 5 elements
        Assertions.assertEquals(5, actualStatusHistory.size(), "Check length");

//        //  ... 1st initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.IN_VALIDATION)
                        .activeFrom(notificationCreatedAt)
                        .relatedTimelineElements(List.of())
                        .build(),
                actualStatusHistory.get(0),
                "1st status wrong"
        );
//
//        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );
//
//        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackKOFirstRecipientTimelineElement", "feedbackKOSecondRecipientTimelineElement",
                                "feedbackKOThirdRecipientTimelineElement", "unreachableFirstRecipientTimelineElement",
                                "unreachableSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
//
//
//        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.UNREACHABLE)
                        .activeFrom(unreachableThirdRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("unreachableThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5th status wrong"
        );
    }


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
                .relatedTimelineElements(Arrays.asList("el3", "el4", "el5", "el6", "el7"))
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
        List<NotificationStatusHistoryElementInt> historyElementList = Arrays.asList(historyElement, historyElement1,
                historyElement2, historyElement4_1, historyElement5, historyElement6);

        // chiamare metodo di test
        List<NotificationStatusHistoryElementInt> resHistoryElementList = statusUtils.getStatusHistory(
                timelineElementList, 1,
                notificationCreatedAt
        );
        // verificare che è risultato atteso
        Assertions.assertEquals(historyElementList, resHistoryElementList);
    }

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

    @Test
    void emptyTimelineInitialStateTest() {
        //
        Assertions.assertEquals(NotificationStatusInt.IN_VALIDATION, statusUtils.getCurrentStatus(Collections.emptyList()));
    }

    @Test
    void getCurrentStatusTest() {
        List<NotificationStatusHistoryElementInt> statusHistory = new ArrayList<>();
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
                .category(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(timelineElement1,
                timelineElement2, timelineElement3);

        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:23:00.00Z");

        Mockito.when(pnDeliveryPushConfigs.getPaperMessageNotHandled()).thenReturn(Boolean.FALSE);
        List<NotificationStatusHistoryElementInt> responseList = statusUtils.getStatusHistory(timelineElementList, 3, notificationCreatedAt);

        Assertions.assertEquals(responseList.size(), 3);
    }

}