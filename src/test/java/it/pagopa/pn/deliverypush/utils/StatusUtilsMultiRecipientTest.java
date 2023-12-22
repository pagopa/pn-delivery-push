package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.model.Get;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class StatusUtilsMultiRecipientTest {

    private StatusUtils statusUtils;

    @BeforeEach
    public void setup() {
        this.statusUtils = new StatusUtils();
    }
    
    @Test
    void moreRecipientUnreachableAndDelivered() {
        // GIVEN

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .elementId("requestAccepted")
                .timestamp((Instant.parse("2021-09-16T15:24:00.00Z")))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        //Primo recipient
        TimelineElementInternal getAddressRec1 = TimelineElementInternal.builder()
                .elementId("getAddressRec1")
                .timestamp((Instant.parse("2021-09-16T15:24:30.00Z")))
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .build();

        TimelineElementInternal sendDigitalDomicileRec1 = TimelineElementInternal.builder()
                .elementId("sendDigitalDomicileRec1")
                .timestamp((Instant.parse("2021-09-16T15:25:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();

        TimelineElementInternal digitalDeliveryCreationRequest = TimelineElementInternal.builder()
                .elementId("digitalDeliveryCreationRequestRec1")
                .timestamp((Instant.parse("2021-09-16T15:31:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST)
                .build();

        // Secondo recipient
        TimelineElementInternal getAddressRec2 = TimelineElementInternal.builder()
                .elementId("getAddressRec2")
                .timestamp((Instant.parse("2021-09-16T17:29:30.00Z")))
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .build();
        
        TimelineElementInternal sendAnalogDomicileRec2 = TimelineElementInternal.builder()
                .elementId("sendAnalogDomicileRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();

        TimelineElementInternal sendAnalogFeedbackRec2 = TimelineElementInternal.builder()
                .elementId("sendAnalogFeedbackRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:05.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(2).notificationDate(Instant.parse("2021-09-16T17:30:05.00Z")).build())
                .build();
        
        TimelineElementInternal completelyUnreachableRec2 = TimelineElementInternal.builder()
                .elementId("completelyUnreachableRec2")
                .timestamp((Instant.parse("2021-09-16T17:31:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetailsInt.builder().recIndex(2).build())
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(
                requestAccepted,
                getAddressRec1,
                sendDigitalDomicileRec1,
                digitalDeliveryCreationRequest,
                getAddressRec2,
                sendAnalogDomicileRec2,
                sendAnalogFeedbackRec2,
                completelyUnreachableRec2
        );


        // creare List<NotificationStatusHistoryElementInt>
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        NotificationStatusHistoryElementInt historyInValidation = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(notificationCreatedAt)
                .relatedTimelineElements(List.of())
                .build();

        NotificationStatusHistoryElementInt historyAccepted = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom(requestAccepted.getTimestamp())
                .relatedTimelineElements(
                        Arrays.asList(
                                requestAccepted.getElementId(),
                                getAddressRec1.getElementId()
                        )
                )
                .build();

        NotificationStatusHistoryElementInt historyDelivering = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom(sendDigitalDomicileRec1.getTimestamp())
                .relatedTimelineElements(
                        Arrays.asList(
                                sendDigitalDomicileRec1.getElementId(),
                                digitalDeliveryCreationRequest.getElementId(),
                                getAddressRec2.getElementId(),
                                sendAnalogDomicileRec2.getElementId(),
                                sendAnalogFeedbackRec2.getElementId()
                        )
                )
                .build();

        //La businessDate di COMPLETELY_UNREACHABLE è il notificationDate dell'ultimo SEND_ANALOG_FEEDBACK
        Instant CompleteUnreachableBusinessDate = sendAnalogFeedbackRec2.getTimestamp();

        NotificationStatusHistoryElementInt historyDelivered = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERED)
                .activeFrom(CompleteUnreachableBusinessDate)
                .relatedTimelineElements(List.of(completelyUnreachableRec2.getElementId()))
                .build();

        List<NotificationStatusHistoryElementInt> historyElementList = Arrays.asList(historyInValidation, historyAccepted,
                historyDelivering, historyDelivered);

        // WHEN
        List<NotificationStatusHistoryElementInt> resHistoryElementList = statusUtils.getStatusHistory(
                timelineElementList, 2,
                notificationCreatedAt
        );

        // THEN
        Assertions.assertEquals(historyElementList, resHistoryElementList);
    }


    @Test
    void firstRecipientUnreachableAndSecondDelivered() {
        // GIVEN

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .elementId("requestAccepted")
                .timestamp((Instant.parse("2021-09-16T15:24:00.00Z")))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        //Primo recipient
        TimelineElementInternal getAddressRec1 = TimelineElementInternal.builder()
                .elementId("getAddressRec1")
                .timestamp((Instant.parse("2021-09-16T15:24:30.00Z")))
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .details(GetAddressInfoDetailsInt.builder().recIndex(0).build())
                .build();

        TimelineElementInternal sendDigitalDomicileRec1 = TimelineElementInternal.builder()
                .elementId("sendDigitalDomicileRec1")
                .timestamp((Instant.parse("2021-09-16T17:30:10.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(SendDigitalDetailsInt.builder().recIndex(0).build())
                .build();

        TimelineElementInternal sendDigitalFeedbackRec1 = TimelineElementInternal.builder()
                .elementId("sendDigitalFeedbackRec1")
                .timestamp((Instant.parse("2021-09-16T17:30:15.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(Instant.parse("2021-09-16T17:30:15.00Z")).build())
                .build();

        TimelineElementInternal digitalDeliveryCreationRequest = TimelineElementInternal.builder()
                .elementId("digitalDeliveryCreationRequestRec1")
                .timestamp((Instant.parse("2021-09-16T17:30:20.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST)
                .details(DigitalDeliveryCreationRequestDetailsInt.builder().recIndex(0).build())
                .build();



        // Secondo recipient
        TimelineElementInternal getAddressRec2 = TimelineElementInternal.builder()
                .elementId("getAddressRec2")
                .timestamp((Instant.parse("2021-09-16T17:29:30.00Z")))
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .details(GetAddressInfoDetailsInt.builder().recIndex(1).build())
                .build();

        TimelineElementInternal sendAnalogDomicileRec2 = TimelineElementInternal.builder()
                .elementId("sendAnalogDomicileRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder().recIndex(1).build())
                .build();

        TimelineElementInternal sendAnalogFeedbackRec2 = TimelineElementInternal.builder()
                .elementId("sendAnalogFeedbackRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:05.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(Instant.parse("2021-09-16T17:30:05.00Z")).build())
                .build();

        TimelineElementInternal completelyUnreachableRec2 = TimelineElementInternal.builder()
                .elementId("completelyUnreachableRec2")
                .timestamp((Instant.parse("2021-09-16T17:31:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetailsInt.builder().recIndex(1).build())
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(
                requestAccepted,
                getAddressRec1,
                sendDigitalDomicileRec1,
                sendDigitalFeedbackRec1,
                digitalDeliveryCreationRequest,
                getAddressRec2,
                sendAnalogDomicileRec2,
                sendAnalogFeedbackRec2,
                completelyUnreachableRec2
        );


        // creare List<NotificationStatusHistoryElementInt>
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        NotificationStatusHistoryElementInt historyInValidation = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(notificationCreatedAt)
                .relatedTimelineElements(List.of())
                .build();

        NotificationStatusHistoryElementInt historyAccepted = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom(requestAccepted.getTimestamp())
                .relatedTimelineElements(
                        Arrays.asList(
                                requestAccepted.getElementId(),
                                getAddressRec1.getElementId(),
                                getAddressRec2.getElementId()
                        )
                )
                .build();

        NotificationStatusHistoryElementInt historyDelivering = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom(sendAnalogDomicileRec2.getTimestamp())
                .relatedTimelineElements(
                        Arrays.asList(
                                sendAnalogDomicileRec2.getElementId(),
                                sendAnalogFeedbackRec2.getElementId(),
                                completelyUnreachableRec2.getElementId(),
                                sendDigitalDomicileRec1.getElementId(),
                                sendDigitalFeedbackRec1.getElementId()
                        )
                )
                .build();

        NotificationStatusHistoryElementInt historyDelivered = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERED)
                .activeFrom(digitalDeliveryCreationRequest.getTimestamp())
                .relatedTimelineElements(List.of(digitalDeliveryCreationRequest.getElementId()))
                .build();

        List<NotificationStatusHistoryElementInt> historyElementList = Arrays.asList(historyInValidation, historyAccepted,
                historyDelivering, historyDelivered);

        // WHEN
        List<NotificationStatusHistoryElementInt> resHistoryElementList = statusUtils.getStatusHistory(
                timelineElementList, 2,
                notificationCreatedAt
        );

        // THEN
        Assertions.assertEquals(historyElementList, resHistoryElementList);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void moreRecipientUnreachableAndDeliveredButViewed() {

        // GIVEN
        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .elementId("requestAccepted")
                .timestamp((Instant.parse("2021-09-16T15:24:00.00Z")))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        //Primo recipient
        TimelineElementInternal getAddressRec1 = TimelineElementInternal.builder()
                .elementId("getAddressRec1")
                .timestamp((Instant.parse("2021-09-16T15:24:30.00Z")))
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .build();

        TimelineElementInternal sendDigitalDomicileRec1 = TimelineElementInternal.builder()
                .elementId("sendDigitalDomicileRec1")
                .timestamp((Instant.parse("2021-09-16T15:25:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .build();

        TimelineElementInternal digitalSuccessWorkflowRec1 = TimelineElementInternal.builder()
                .elementId("digitalSuccessWorkflowRec1")
                .timestamp((Instant.parse("2021-09-16T15:31:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();

        // Secondo recipient
        TimelineElementInternal sendAnalogDomicileRec2 = TimelineElementInternal.builder()
                .elementId("sendAnalogDomicileRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();

        TimelineElementInternal sendAnalogFeedbackRec2 = TimelineElementInternal.builder()
                .elementId("sendAnalogFeedbackRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:05.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(Instant.parse("2021-09-16T17:30:05.00Z")).build())
                .build();

        TimelineElementInternal viewedRec2 = TimelineElementInternal.builder()
                .elementId("viewedRec2")
                .timestamp((Instant.parse("2021-09-16T17:30:30.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();

        TimelineElementInternal completelyUnreachableRec2 = TimelineElementInternal.builder()
                .elementId("completelyUnreachableRec2")
                .timestamp((Instant.parse("2021-09-16T17:31:00.00Z")))
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetailsInt.builder().recIndex(1).build())
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(
                requestAccepted,
                getAddressRec1,
                sendDigitalDomicileRec1,
                digitalSuccessWorkflowRec1,
                sendAnalogDomicileRec2,
                sendAnalogFeedbackRec2,
                viewedRec2,
                completelyUnreachableRec2
        );


        // creare List<NotificationStatusHistoryElementInt>
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        NotificationStatusHistoryElementInt historyInValidation = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(notificationCreatedAt)
                .relatedTimelineElements(List.of())
                .build();

        NotificationStatusHistoryElementInt historyAccepted = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom(requestAccepted.getTimestamp())
                .relatedTimelineElements(
                        Arrays.asList(
                                requestAccepted.getElementId(),
                                getAddressRec1.getElementId()
                        )
                )
                .build();

        NotificationStatusHistoryElementInt historyDelivering = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom(sendDigitalDomicileRec1.getTimestamp())
                .relatedTimelineElements(
                        Arrays.asList(
                                sendDigitalDomicileRec1.getElementId(),
                                digitalSuccessWorkflowRec1.getElementId(),
                                sendAnalogDomicileRec2.getElementId(),
                                sendAnalogFeedbackRec2.getElementId(),
                                completelyUnreachableRec2.getElementId() //Poichè il timestamp viene modificata con la data di business del SEND_ANALOG_FEEDBACK
                        )
                )
                .build();

        NotificationStatusHistoryElementInt historyDelivered = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.VIEWED)
                .activeFrom(viewedRec2.getTimestamp())
                .relatedTimelineElements(List.of(
                        viewedRec2.getElementId()
                ))
                .build();

        List<NotificationStatusHistoryElementInt> historyElementList = Arrays.asList(historyInValidation, historyAccepted,
                historyDelivering, historyDelivered);

        // WHEN
        List<NotificationStatusHistoryElementInt> resHistoryElementList = statusUtils.getStatusHistory(
                timelineElementList, 2,
                notificationCreatedAt
        );

        // THEN
        Assertions.assertEquals(historyElementList, resHistoryElementList);
    }
}
