package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

class StatusUtilsTest {
    @Mock
    private TimelineService timelineService;
    
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private StatusUtils statusUtils;

    @BeforeEach
    public void setup() {
        this.statusUtils = new StatusUtils();
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

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

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

    @Test
    void checkStatusNotDeliveredWithDigitalFailure() {

        // GIVEN a timeline
        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .elementId("el1")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendDigitalDomicile = TimelineElementInternal.builder()
                .elementId("el3")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendDigitalFeedback = TimelineElementInternal.builder()
                .elementId("el4")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal digitalFailureWorkflow = TimelineElementInternal.builder()
                .elementId("el5")
                .timestamp((Instant.parse("2021-09-16T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW)
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAccepted, sendDigitalDomicile,
                sendDigitalFeedback, digitalFailureWorkflow);


        // WHEN ask for status history
        Instant notificationCreatedAt = requestAccepted.getTimestamp().minus(Duration.ofHours(1));

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList, 
                1,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have same length
        Assertions.assertEquals(3, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAccepted.getTimestamp())
                        .relatedTimelineElements(List.of(requestAccepted.getElementId()))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendDigitalDomicile.getTimestamp())
                        .relatedTimelineElements(Arrays.asList(
                                sendDigitalDomicile.getElementId(),
                                sendDigitalFeedback.getElementId(),
                                digitalFailureWorkflow.getElementId())
                        )
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
    }

    @Test
    void checkStatusDeliveredWithRegisteredLetter() {

        // GIVEN a timeline
        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .elementId("el1")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendDigitalDomicile = TimelineElementInternal.builder()
                .elementId("el3")
                .timestamp((Instant.parse("2021-09-16T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendDigitalFeedback = TimelineElementInternal.builder()
                .elementId("el4")
                .timestamp((Instant.parse("2021-09-16T15:27:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal digitalFailureWorkflow = TimelineElementInternal.builder()
                .elementId("el5")
                .timestamp((Instant.parse("2021-09-16T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW)
                .build();
        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .elementId("el6")
                .timestamp((Instant.parse("2021-09-16T15:29:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAccepted, sendDigitalDomicile,
                sendDigitalFeedback, digitalFailureWorkflow, sendSimpleRegisteredLetter);


        // WHEN ask for status history
        Instant notificationCreatedAt = requestAccepted.getTimestamp().minus(Duration.ofHours(1));

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                1,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have same length
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAccepted.getTimestamp())
                        .relatedTimelineElements(List.of(requestAccepted.getElementId()))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendDigitalDomicile.getTimestamp())
                        .relatedTimelineElements(Arrays.asList(
                                sendDigitalDomicile.getElementId(),
                                sendDigitalFeedback.getElementId(),
                                digitalFailureWorkflow.getElementId())
                        )
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(sendSimpleRegisteredLetter.getTimestamp())
                        .relatedTimelineElements(List.of(sendSimpleRegisteredLetter.getElementId()))
                        .build(),
                actualStatusHistory.get(3),
                "4rd status wrong"
        );
    }
    
    // IN_VALIDATION - ACCEPTED - DELIVERING
    // un destinatario è in fase di SEND_DIGITAL_DOMICILE
    @Test
    void getTimelineHistoryMultiRecipientWithOneSendDigitalDomicileTest() {
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

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement);

        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 5 elements
        Assertions.assertEquals(3, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
    }

    // IN_VALIDATION - ACCEPTED - DELIVERING - VIEWED - PAID
    // tutti e 3 destinatari sono raggiungibili via domicilio digitale ma un destinatario visualizza la notifica sul portale di PN
    // prima che la visualizzi su PEC, e poi la paga. Stato finale: PAID
    @Test
    void getTimelineHistoryMultiRecipientWithOneViewViaPNTest() {
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

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 5 elements
        Assertions.assertEquals(5, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
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

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement","pecReceivedFirstRecipientTimelineElement", "pecReceivedSecondRecipientTimelineElement", "pecReceivedThirdRecipientTimelineElement", "viewedFromPecTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.PAID)
                        .activeFrom(paymentTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("paymentTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5th status wrong"
        );
    }

    // IN_VALIDATION - ACCEPTED - DELIVERING - VIEWED - PAID
    // tutti e 3 destinatari sono raggiungibili via domicilio digitale ma un destinatario visualizza la notifica sul portale di PN
    // prima che la visualizzi su PEC (ma dopo che ExtChannels dà feedback positivo per tutti e 3), e la paga.
    // Successivamente, tutti e 3 ricevono la PEC e gli altri 2 la visualizzano. Stato finale: PAID
    @Test
    void getTimelineHistoryMultiRecipientWithOnePayedBeforeAllWorkflowsCompletedTest() {
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
        //e poi paga la multa
        TimelineElementInternal paymentTimelineElement = TimelineElementInternal.builder()
                .elementId("paymentTimelineElement")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();
        //tutti e 3 destinatari ricevono con successo la notifica via PEC
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T18:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T18:33:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T18:34:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal viewedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T18:35:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal viewedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T18:36:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement,
                sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement, feedbackOKFirstRecipientTimelineElement,
                feedbackOKSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, viewedFromPNTimelineElement,
                paymentTimelineElement, pecReceivedFirstRecipientTimelineElement, pecReceivedSecondRecipientTimelineElement,
                pecReceivedThirdRecipientTimelineElement, viewedSecondRecipientTimelineElement, viewedThirdRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 5 elements
        Assertions.assertEquals(5, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
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

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.PAID)
                        .activeFrom(paymentTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("paymentTimelineElement", "pecReceivedFirstRecipientTimelineElement",
                                "pecReceivedSecondRecipientTimelineElement", "pecReceivedThirdRecipientTimelineElement",
                                "viewedSecondRecipientTimelineElement", "viewedThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5th status wrong"
        );
    }

    // IN_VALIDATION - ACCEPTED - VIEWED - PAID
    // tutti e 3 destinatari sono raggiungibili via domicilio digitale ma un destinatario visualizza la notifica sul portale di PN
    // prima che la visualizzi su PEC (e prima che ExtChannels dà feedback positivo per tutti e 3), e la paga.
    // Successivamente, tutti e 3 ricevono la PEC e gli altri 2 la visualizzano. Stato finale: PAID
    @Test
    void getTimelineHistoryMultiRecipientWithOnePayedBeforeAllFeedbackOKFromExternalChannelsTest() {
        final int NUMBER_OF_RECIPIENTS = 3;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        //uno dei 3 destinatari riceve un avviso di cortesia
        TimelineElementInternal sendCourtesyMessage = TimelineElementInternal.builder()
                .elementId("sendCourtesyMessage")
                .timestamp((Instant.parse("2021-09-16T15:25:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .build();
        //lo stesso destinatario visualizza la notifica
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        //un altro destinatario riceve un avviso di cortesia
        TimelineElementInternal sendCourtesySecondMessage = TimelineElementInternal.builder()
                .elementId("sendCourtesySecondMessage")
                .timestamp((Instant.parse("2021-09-16T15:35:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .build();
        //il primo destinatario (che ha visualizzato la notifica), la paga
        TimelineElementInternal paymentTimelineElement = TimelineElementInternal.builder()
                .elementId("paymentTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:42:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:43:40.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        //per tutti e 3 arriva il feedback positivo di external channels
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:44:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:45:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:46:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        //tutti e 3 destinatari ricevono con successo la notifica via PEC
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:50:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:51:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:52:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //i 3 destinatari visualizzano la notifica via PEC
        TimelineElementInternal viewedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:53:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal viewedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:54:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal viewedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:55:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(  requestAcceptedTimelineElement, sendCourtesyMessage, viewedFromPNTimelineElement,
                paymentTimelineElement, sendCourtesySecondMessage, sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement,
                sendPecThirdRecipientTimelineElement, feedbackOKFirstRecipientTimelineElement, feedbackOKSecondRecipientTimelineElement,
                feedbackOKThirdRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement, pecReceivedSecondRecipientTimelineElement,
                pecReceivedThirdRecipientTimelineElement, viewedSecondRecipientTimelineElement, viewedThirdRecipientTimelineElement,
                viewedFirstRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement", "sendCourtesyMessage"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement", "sendCourtesySecondMessage"))
                        .build(),
                actualStatusHistory.get(2),
                "3th status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.PAID)
                        .activeFrom(paymentTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("paymentTimelineElement", "sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "feedbackOKSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement",
                                "pecReceivedSecondRecipientTimelineElement", "pecReceivedThirdRecipientTimelineElement",
                                "viewedSecondRecipientTimelineElement", "viewedThirdRecipientTimelineElement",
                                "viewedFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    // IN_VALIDATION - ACCEPTED - DELIVERING - VIEWED
    // NOTA: lo stato non passa da DELIVERING a DELIVERED quando viene completato il workflow per il primo destinatario
    // poiché gli altri 2 destinatari non hanno completato il workflow.

    // tutti e 3 destinatari sono raggiungibili via domicilio digitale, un destinatario visualizza la notifica dalla PEC
    // prima che PN riceva un feedback positivo da External Channels per gli altri 2 destinatari. Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientWithOneViewViaPecWithOneCompleteWorkflowTest() {
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
        //PN riceve feedback positivo da External Channels per uno dei destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //Questo stesso destinatario visualizza la notifica via PEC
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:35:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        //PN riceve feedback positivo da External Channels per gli altri 2 destinatari
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:41:10.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                feedbackOKFirstRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement, viewedFromPNTimelineElement,
                feedbackOKSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, pecReceivedSecondRecipientTimelineElement,
                pecReceivedThirdRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement", "feedbackOKSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement", "pecReceivedSecondRecipientTimelineElement",
                                "pecReceivedThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    // IN_VALIDATION - ACCEPTED - DELIVERING - DELIVERED - VIEWED
    // tutti e 3 destinatari sono raggiungibili via domicilio digitale, un destinatario visualizza la notifica dalla PEC
    // dopo che il workflow è completo per tutti e 3. Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientWithOneViewViaPecWithAllCompleteWorkflowTest() {
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
        //PN riceve feedback positivo da External Channels per tutti e 3 destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //PN riceve feedback positivo da External Channels per tutti e 3 destinatari e il workflow si completa per tutti e 3
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal feedbackOKThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:35:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:36:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //Un destinatario visualizza la notifica
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:50:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                feedbackOKFirstRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement, feedbackOKSecondRecipientTimelineElement,
                pecReceivedSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, pecReceivedThirdRecipientTimelineElement,
                viewedFromPNTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

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

        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement",
                                "feedbackOKSecondRecipientTimelineElement", "pecReceivedSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(pecReceivedThirdRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("pecReceivedThirdRecipientTimelineElement"))
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

    // IN_VALIDATION - ACCEPTED - DELIVERING - VIEWED
    // tutti e 3 destinatari sono raggiungibili via domicilio digitale, un destinatario visualizza la notifica dalla PEC
    // dopo che External Channels ha dato feedback positivo per tutti e 3, ma prima che gli altri 2 destinatari ricevano
    // la PEC (e quindi il workflow non è completo per questi 2). Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientWithOneViewViaPecWithOneCompleteWorkflowWithAllFeedbacksTest() {
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
        //PN riceve feedback positivo da External Channels per tutti e 3 destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //PN riceve feedback positivo da External Channels per tutti e 3 destinatari e il workflow si completa per tutti e 3
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        //Un destinatario visualizza la notifica prima che il workflow sia completo per gli altri 2 destinatari
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:35:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:36:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();

        TimelineElementInternal pecReceivedThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:37:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                feedbackOKFirstRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement, feedbackOKSecondRecipientTimelineElement,
                feedbackOKThirdRecipientTimelineElement, viewedFromPNTimelineElement, pecReceivedSecondRecipientTimelineElement ,
                pecReceivedThirdRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

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

        //  ... 2nd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.ACCEPTED)
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement",
                                "feedbackOKSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );


        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement",
                                "pecReceivedSecondRecipientTimelineElement", "pecReceivedThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
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


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement,
                sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement, feedbackKOFirstRecipientTimelineElement,
                feedbackKOSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, unreachableFirstRecipientTimelineElement,
                unreachableSecondRecipientTimelineElement, pecReceivedTimelineElement
                );


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
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

    // IN VALIDATION - ACCEPTED - DELIVERING - DELIVERED
    // Un destinatario su 3 è non raggiungibile, 2 sono raggiungibili, stato finale: DELIVERED
    @Test
    void getTimelineHistoryMultiRecipientWithTwoSuccessTest() {
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

        //2 destinatari ricevono con successo la notifica via PEC, mentre 1 non è raggiungibile
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal unreachableTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:33:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:34:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement,
                sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement, feedbackOKFirstRecipientTimelineElement,
                feedbackKOSecondRecipientTimelineElement, feedbackOKThirdRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement,
                unreachableTimelineElement, pecReceivedSecondRecipientTimelineElement
        );


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "feedbackKOSecondRecipientTimelineElement",
                                "feedbackOKThirdRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement",
                                "unreachableTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );


        // Qui si va in DELIVERED a prescindere dallo status sull'ultimo destinatario, poiché quello che conta è che il workflow
        // sia stato completato anche per questo destinatario e uno dei destinatari abbia già ricevuto in precedenza, con successo, la notifica
        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(pecReceivedSecondRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("pecReceivedSecondRecipientTimelineElement"))
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

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
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

        //  ... 4th initial status
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
    // tutti e 3 destinatari non sono raggiungibili ma 1 dei 3 visualizza la notifica su PN, DOPO che PN ha ricevuto i
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

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 5 elements
        Assertions.assertEquals(5, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
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

        //  ... 4th initial status
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

    // IN VALIDATION - ACCEPTED - DELIVERING - VIEWED
    // tutti e 3 destinatari non sono raggiungibili ma 1 dei 3 visualizza la notifica su PN, PRIMA che PN ha ricevuto i
    // feedback negativi da External Channels. Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientWithAllUnreachableButOneViewedBeforeKOFeedbackFromPNTest() {
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
        TimelineElementInternal viewedFromPNTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedFromPNTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:27:10.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal feedbackKOSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:28:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackKOThirdRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOThirdRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:28:30.00Z")))
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


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendPecFirstRecipientTimelineElement,
                sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement, feedbackKOFirstRecipientTimelineElement,
                viewedFromPNTimelineElement, feedbackKOSecondRecipientTimelineElement, feedbackKOThirdRecipientTimelineElement,
                unreachableFirstRecipientTimelineElement, unreachableSecondRecipientTimelineElement, unreachableThirdRecipientTimelineElement
        );


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
        Assertions.assertEquals(4, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "feedbackKOFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedFromPNTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedFromPNTimelineElement",
                                "feedbackKOSecondRecipientTimelineElement", "feedbackKOThirdRecipientTimelineElement",
                                "unreachableFirstRecipientTimelineElement", "unreachableSecondRecipientTimelineElement",
                                "unreachableThirdRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    // IN VALIDATION - ACCEPTED - DELIVERING
    // per 2 dei 3 destinatari vien generato l'AAR dopo il SEND_DIGITAL_DOMICILE. Stato finale: DELIVERING
    @Test
    void getTimelineHistoryMultiRecipientAARGenerationTest() {
        final int NUMBER_OF_RECIPIENTS = 3;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal courtesyFirstTimelineElement = TimelineElementInternal.builder()
                .elementId("courtesyFirstTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:25:00.00Z"))
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .build();
        TimelineElementInternal courtesySecondTimelineElement = TimelineElementInternal.builder()
                .elementId("courtesySecondTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:25:10.00Z"))
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .build();
        TimelineElementInternal courtesyThirdTimelineElement = TimelineElementInternal.builder()
                .elementId("courtesyThirdTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:25:30.00Z"))
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .build();
        TimelineElementInternal firstAARTimelineElement = TimelineElementInternal.builder()
                .elementId("firstAARTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:26:00.00Z"))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:10.00Z")))
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
        TimelineElementInternal secondAARTimelineElement = TimelineElementInternal.builder()
                .elementId("secondAARTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:30:00.00Z"))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .build();
        TimelineElementInternal thirdAARTimelineElement = TimelineElementInternal.builder()
                .elementId("thirdAARTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:31:00.00Z"))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, courtesyFirstTimelineElement,
                courtesySecondTimelineElement, courtesyThirdTimelineElement, firstAARTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, sendPecThirdRecipientTimelineElement,
                secondAARTimelineElement, thirdAARTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 3 elements
        Assertions.assertEquals(3, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement", "courtesyFirstTimelineElement",
                                "courtesySecondTimelineElement", "courtesyThirdTimelineElement", "firstAARTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "sendPecThirdRecipientTimelineElement",
                                "secondAARTimelineElement", "thirdAARTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );
    }

    // IN VALIDATION - ACCEPTED - DELIVERING - DELIVERED - EFFECTIVE_DATE - VIEWED
    // 2 destinatari non leggono la notifica entro la data di perfezionamento per decorrenza termini
    // poi uno la visualizza. Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientEffectiveDateAndViewedTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal feedbackOKTFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:28:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:29:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:29:30.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal refinementFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("refinementFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.REFINEMENT)
                .build();
        TimelineElementInternal refinementSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("refinementSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:30.00Z")))
                .category(TimelineElementCategoryInt.REFINEMENT)
                .build();
        TimelineElementInternal viewedTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:31:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, feedbackOKTFirstRecipientTimelineElement,
                feedbackOKSecondRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement, pecReceivedSecondRecipientTimelineElement,
                refinementFirstRecipientTimelineElement, refinementSecondRecipientTimelineElement,
                viewedTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 6 elements
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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "feedbackOKFirstRecipientTimelineElement",
                                "feedbackOKSecondRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(pecReceivedSecondRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("pecReceivedSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4rd status wrong"
        );

        //  ... 5rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.EFFECTIVE_DATE)
                        .activeFrom(refinementFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("refinementFirstRecipientTimelineElement",
                                "refinementSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5rd status wrong"
        );

        //  ... 6rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedTimelineElement"))
                        .build(),
                actualStatusHistory.get(5),
                "6rd status wrong"
        );
    }

    // IN VALIDATION - ACCEPTED - DELIVERING - DELIVERED - VIEWED
    // 1 destinatario legge la notifica via PEC, il secondo non legge la notifica entro la data di perfezionamento per decorrenza termini
    // Stato finale: VIEWED
    @Test
    void getTimelineHistoryMultiRecipientViewedAndAfterEffectiveDateTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-16T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();
        TimelineElementInternal feedbackOKTFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal feedbackOKSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:28:30.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:29:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal pecReceivedSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:29:30.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        TimelineElementInternal viewedTimelineElement = TimelineElementInternal.builder()
                .elementId("viewedTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();
        TimelineElementInternal refinementTimelineElement = TimelineElementInternal.builder()
                .elementId("refinementTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:31:30.00Z")))
                .category(TimelineElementCategoryInt.REFINEMENT)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement, feedbackOKTFirstRecipientTimelineElement,
                feedbackOKSecondRecipientTimelineElement, pecReceivedFirstRecipientTimelineElement, pecReceivedSecondRecipientTimelineElement,
                viewedTimelineElement, refinementTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object(){}.getClass().getEnclosingMethod().getName());

        // THEN status histories have 5 elements
        Assertions.assertEquals(5, actualStatusHistory.size(), "Check length");

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
                        .activeFrom(requestAcceptedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("requestAcceptedTimelineElement"))
                        .build(),
                actualStatusHistory.get(1),
                "2nd status wrong"
        );

        //  ... 3rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERING)
                        .activeFrom(sendPecFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement",
                                "sendPecSecondRecipientTimelineElement", "feedbackOKFirstRecipientTimelineElement",
                                "feedbackOKSecondRecipientTimelineElement", "pecReceivedFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(pecReceivedSecondRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("pecReceivedSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4rd status wrong"
        );

        //  ... 5rd initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(viewedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("viewedTimelineElement", "refinementTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5rd status wrong"
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
    @ExtendWith(SpringExtension.class)
    void getCurrentStatusFromNotification() {
        NotificationRecipientInt recipient = NotificationRecipientInt.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("test")
                .withNotificationRecipient(recipient)
                .build();
        
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

        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(timelineElementList);


        NotificationStatusInt response = statusUtils.getCurrentStatusFromNotification(notification, timelineService);

        Assertions.assertEquals( NotificationStatusInt.VIEWED, response);
    }
    
    private void printStatus(List<NotificationStatusHistoryElementInt> notificationHistoryElements, String methodName) {
        System.out.print(methodName + " - ");
        notificationHistoryElements.stream()
                .map(NotificationStatusHistoryElementInt::getStatus)
                .forEach(notificationStatusInt -> System.out.print(notificationStatusInt + " "));
        System.out.println();
    }

}