package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineMapperFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;

class StatusUtilsMultiRecipientTest {

    private StatusUtils statusUtils;
    private static final String SERCQ_ADDRESS = "x-pagopa-pn-sercq:send-self:notification-already-delivered";
    private static final String PEC_ADDRESS = "test@pec.it";

    @BeforeEach
    public void setup() {
        PnDeliveryPushConfigs pnDeliveryPushConfigs = mock(PnDeliveryPushConfigs.class);
        FeatureEnabledUtils featureEnabledUtils = mock(FeatureEnabledUtils.class);
        this.statusUtils = new StatusUtils(new SmartMapper(new TimelineMapperFactory(pnDeliveryPushConfigs), featureEnabledUtils));
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
                .details(getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC))
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
                .details(getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC))
                .build();

        TimelineElementInternal sendDigitalFeedbackRec1 = TimelineElementInternal.builder()
                .elementId("sendDigitalFeedbackRec1")
                .timestamp((Instant.parse("2021-09-16T17:30:15.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .details(SendDigitalFeedbackDetailsInt.builder().recIndex(0).notificationDate(Instant.parse("2021-09-16T17:30:15.00Z")).build())
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
                .details(getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC))
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
                .details(NotificationViewedDetailsInt.builder().recIndex(1).build())
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

    private SendDigitalDetailsInt getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE addressType) {

        String address = addressType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC ? PEC_ADDRESS : SERCQ_ADDRESS;
        LegalDigitalAddressInt legalDigitalAddressIntPec = LegalDigitalAddressInt.builder()
                .type(addressType).address(address).build();
        SendDigitalDetailsInt sendDigitalDetailsIntPec = new SendDigitalDetailsInt();
        sendDigitalDetailsIntPec.setDigitalAddress(legalDigitalAddressIntPec);
        return sendDigitalDetailsIntPec;
    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - DELIVERED
        un destinatario è raggiungibile via domicilio digitale e per l'altro viene ricevuta una comunicazione di decesso
        Stato finale: DELIVERED
    */
    @Test
    void getTimelineHistoryMultiRecipientWithOneDeliveryViaPecWithOneDeceasedWorkflowTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        SendDigitalDetailsInt sendDigitalDetailsIntSercq = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ);


        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        TimelineElementInternal sendPecSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .details(sendDigitalDetailsIntSercq)
                .build();
        //PN riceve feedback positivo da External Channels per uno dei destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal digitalDeliveryRequestFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("digitalDeliveryRequestFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //PN riceve feedback positivo da External Channels ma con deliveryFailureCause M02 (DECEDUTO) per l'altro destinatario
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant deacesedSendAnalogFeedbackBusinessDate = Instant.parse("2021-09-17T06:40:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).build())
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, sendPecSecondRecipientTimelineElement,
                feedbackOKFirstRecipientTimelineElement, digitalDeliveryRequestFirstRecipientTimelineElement,
                pecReceivedFirstRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement, feedbackAnalogSecondRecipientTimelineElement,
                deceasedWorkflowSecondRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

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
                                "sendPecSecondRecipientTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "digitalDeliveryRequestFirstRecipientTimelineElement",
                                "pecReceivedFirstRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement", "feedbackAnalogSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(deacesedSendAnalogFeedbackBusinessDate)
                        .relatedTimelineElements(List.of(
                                "deceasedWorkflowSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - UNREACHABLE
        un destinatario è raggiungibile via domicilio digitale e per l'altro viene ricevuta una comunicazione di decesso
        Stato finale: UNREACHABLE
     */
    @Test
    void getTimelineHistoryMultiRecipientWithOneUnreachableWithOneDeceasedWorkflowTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendAnalogFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        TimelineElementInternal feedbackKOFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackKOFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(Instant.parse("2021-09-13T06:30:00.00Z")).build())
                .build();
        TimelineElementInternal unreachableFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("unreachableFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-14T15:32:00.00Z")))
                .iun("iun1")
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetailsInt.builder().recIndex(0).build())
                .build();
        TimelineElementInternal prepareAnalogDomicileSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("prepareAnalogDomicileSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();

        //PN riceve feedback positivo da External Channels per gli altri 2 destinatari
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant deceasedSendAnalogFeedbackBusinessDate = Instant.parse("2021-09-18T09:30:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .iun("iun2")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(deceasedSendAnalogFeedbackBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(deceasedSendAnalogFeedbackBusinessDate).build())
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendAnalogFirstRecipientTimelineElement,
                feedbackKOFirstRecipientTimelineElement, unreachableFirstRecipientTimelineElement,
                prepareAnalogDomicileSecondRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement,
                feedbackAnalogSecondRecipientTimelineElement, deceasedWorkflowSecondRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

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
                        .activeFrom(sendAnalogFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendAnalogFirstRecipientTimelineElement",
                                "feedbackKOFirstRecipientTimelineElement", "unreachableFirstRecipientTimelineElement",
                                "prepareAnalogDomicileSecondRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement",
                                "feedbackAnalogSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.UNREACHABLE)
                        .activeFrom(deceasedSendAnalogFeedbackBusinessDate)
                        .relatedTimelineElements(List.of(
                                "deceasedWorkflowSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - RETURNED_TO_SENDER
        per entrambi i destinatari viene ricevuta una comunicazione di decesso
        Stato finale: RETURNED_TO_SENDER
     */
    @Test
    void getTimelineHistoryMultiRecipientWithTwoDeceasedWorkflowTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendAnalogFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        Instant feedbackFirstRecipientBusinessDate = Instant.parse("2021-09-13T06:30:00.00Z");
        TimelineElementInternal feedbackFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(feedbackFirstRecipientBusinessDate).build())
                .build();
        TimelineElementInternal deceasedWorkflowFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-14T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(0).notificationDate(feedbackFirstRecipientBusinessDate).build())
                .build();
        TimelineElementInternal prepareAnalogDomicileSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("prepareAnalogDomicileSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant feedbackAnalogSecondRecipientBusinessDate = Instant.parse("2021-09-18T09:30:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .iun("iun2")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(feedbackAnalogSecondRecipientBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(feedbackAnalogSecondRecipientBusinessDate).build())
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendAnalogFirstRecipientTimelineElement,
                feedbackFirstRecipientTimelineElement, deceasedWorkflowFirstRecipientTimelineElement,
                prepareAnalogDomicileSecondRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement,
                feedbackAnalogSecondRecipientTimelineElement, deceasedWorkflowSecondRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

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
                        .activeFrom(sendAnalogFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendAnalogFirstRecipientTimelineElement",
                                "feedbackFirstRecipientTimelineElement", "deceasedWorkflowFirstRecipientTimelineElement",
                                "prepareAnalogDomicileSecondRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement",
                                "feedbackAnalogSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.RETURNED_TO_SENDER)
                        .activeFrom(feedbackAnalogSecondRecipientBusinessDate)
                        .relatedTimelineElements(List.of(
                                "deceasedWorkflowSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - RETURNED_TO_SENDER - CANCELLED
        per entrambi i destinatari viene ricevuta una comunicazione di decesso e successivamente viene richiesta la cancellazione
        Stato finale: CANCELLED
     */
    @Test
    void getTimelineHistoryMultiRecipientWithTwoDeceasedWorkflowTestAndCancellation() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);

        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendAnalogFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        Instant feedbackFirstRecipientBusinessDate = Instant.parse("2021-09-13T06:30:00.00Z");
        TimelineElementInternal feedbackFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(feedbackFirstRecipientBusinessDate).build())
                .build();
        TimelineElementInternal deceasedWorkflowFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-14T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(0).notificationDate(feedbackFirstRecipientBusinessDate).build())
                .build();
        TimelineElementInternal prepareAnalogDomicileSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("prepareAnalogDomicileSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant feedbackAnalogSecondRecipientBusinessDate = Instant.parse("2021-09-18T09:30:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .iun("iun2")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(feedbackAnalogSecondRecipientBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:41:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(feedbackAnalogSecondRecipientBusinessDate).build())
                .build();
        TimelineElementInternal cancelRequestTimelineElement = TimelineElementInternal.builder()
                .elementId("cancelRequestTimelineElement")
                .timestamp((Instant.parse("2021-09-19T10:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST)
                .build();
        TimelineElementInternal cancelledTimelineElement = TimelineElementInternal.builder()
                .elementId("cancelledTimelineElement")
                .timestamp((Instant.parse("2021-09-19T10:01:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLED)
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement, sendAnalogFirstRecipientTimelineElement,
                feedbackFirstRecipientTimelineElement, deceasedWorkflowFirstRecipientTimelineElement,
                prepareAnalogDomicileSecondRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement,
                feedbackAnalogSecondRecipientTimelineElement, deceasedWorkflowSecondRecipientTimelineElement,
                cancelRequestTimelineElement, cancelledTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
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
                        .activeFrom(sendAnalogFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendAnalogFirstRecipientTimelineElement",
                                "feedbackFirstRecipientTimelineElement", "deceasedWorkflowFirstRecipientTimelineElement",
                                "prepareAnalogDomicileSecondRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement",
                                "feedbackAnalogSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.RETURNED_TO_SENDER)
                        .activeFrom(feedbackAnalogSecondRecipientBusinessDate)
                        .relatedTimelineElements(List.of(
                                "deceasedWorkflowSecondRecipientTimelineElement", "cancelRequestTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.CANCELLED)
                        .activeFrom(cancelledTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of(
                                "cancelledTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5th status wrong"
        );
    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - DELIVERED
        un destinatario è raggiungibile via domicilio digitale e per l'altro viene ricevuta una comunicazione di decesso,
        inoltre il deceduto visualizza la notifica. Ci aspettiamo che la visualizzazione non influisca sullo stato in questo caso.
        Stato finale: DELIVERED
     */
    @Test
    void getTimelineHistoryMultiRecipientWithOneDeceasedWorkflowAndOneDigitalWorkflowAndAViewFromDeceasedRecTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        SendDigitalDetailsInt sendDigitalDetailsIntSercq = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ);


        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        TimelineElementInternal aarGenerationTimelineElement = TimelineElementInternal.builder()
                .elementId("aarGenerationTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .details(sendDigitalDetailsIntSercq)
                .build();
        //PN riceve feedback positivo da External Channels per uno dei destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal digitalDeliveryRequestFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("digitalDeliveryRequestFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //PN riceve feedback positivo da External Channels ma con deliveryFailureCause M02 (DECEDUTO) per l'altro destinatario
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant deacesedSendAnalogFeedbackBusinessDate = Instant.parse("2021-09-17T06:40:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:45:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).build())
                .build();

        TimelineElementInternal deceasedRecViewedTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedRecViewedTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:35:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(1)
                        .eventTimestamp(Instant.parse("2021-09-17T15:36:00.00Z"))
                        .build())
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, aarGenerationTimelineElement,
                feedbackOKFirstRecipientTimelineElement, digitalDeliveryRequestFirstRecipientTimelineElement,
                pecReceivedFirstRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement, feedbackAnalogSecondRecipientTimelineElement,
                deceasedWorkflowSecondRecipientTimelineElement, deceasedRecViewedTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

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
                                "aarGenerationTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "digitalDeliveryRequestFirstRecipientTimelineElement",
                                "pecReceivedFirstRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement", "feedbackAnalogSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(deacesedSendAnalogFeedbackBusinessDate)
                        .relatedTimelineElements(List.of(
                                "deceasedWorkflowSecondRecipientTimelineElement","deceasedRecViewedTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );
    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - DELIVERED - VIEWED
        un destinatario è raggiungibile via domicilio digitale e per l'altro viene ricevuta una comunicazione di decesso,
        Successivamente Il primo destinatario visualizza la notifica. Ci aspettiamo che la visualizzazione influisca sullo stato.
        Stato finale: VIEWED
     */
    @Test
    void getTimelineHistoryMultiRecipientWithOneDeceasedWorkflowAndOneDigitalWorkflowAndAViewFromAliveRecTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        SendDigitalDetailsInt sendDigitalDetailsIntSercq = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ);


        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        TimelineElementInternal aarGenerationTimelineElement = TimelineElementInternal.builder()
                .elementId("aarGenerationTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .details(sendDigitalDetailsIntSercq)
                .build();
        //PN riceve feedback positivo da External Channels per uno dei destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal digitalDeliveryRequestFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("digitalDeliveryRequestFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();
        //PN riceve feedback positivo da External Channels ma con deliveryFailureCause M02 (DECEDUTO) per l'altro destinatario
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant deacesedSendAnalogFeedbackBusinessDate = Instant.parse("2021-09-17T06:40:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:45:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).build())
                .build();

        // Il primo destinatario (vivo) visualizza la notifica qualche giorno dopo la ricezione dell'evento di deceduto
        TimelineElementInternal firstRecViewedTimelineElement = TimelineElementInternal.builder()
                .elementId("firstRecViewedTimelineElement")
                .timestamp((Instant.parse("2021-09-20T10:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(0)
                        .eventTimestamp(Instant.parse("2021-09-20T10:00:00.00Z"))
                        .build())
                .build();


        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, aarGenerationTimelineElement,
                feedbackOKFirstRecipientTimelineElement, digitalDeliveryRequestFirstRecipientTimelineElement,
                pecReceivedFirstRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement, feedbackAnalogSecondRecipientTimelineElement,
                deceasedWorkflowSecondRecipientTimelineElement, firstRecViewedTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

        // THEN status histories have 4 elements
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
                                "aarGenerationTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "digitalDeliveryRequestFirstRecipientTimelineElement",
                                "pecReceivedFirstRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement", "feedbackAnalogSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.DELIVERED)
                        .activeFrom(deacesedSendAnalogFeedbackBusinessDate)
                        .relatedTimelineElements(List.of(
                                "deceasedWorkflowSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "4th status wrong"
        );

        //  ... 5th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(firstRecViewedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of(
                                "firstRecViewedTimelineElement"))
                        .build(),
                actualStatusHistory.get(4),
                "5th status wrong"
        );
    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - VIEWED
        un destinatario è raggiungibile via domicilio digitale e visualizza la notifica. Per l'altro successivamente
        viene ricevuta una comunicazione di decesso. Ci aspettiamo che la visualizzazione avvenuta in precedenza
        non permetta il passaggio in DELIVERED, poichè VIEWED è uno stato più avanzato.
        Stato finale: VIEWED
     */
    @Test
    void getTimelineHistoryMultiRecipientWithOneDigitalWorkflowAndAViewFromAliveRecBeforeDeceasedWorkflowTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        SendDigitalDetailsInt sendDigitalDetailsIntSercq = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ);


        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendPecFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendPecFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        TimelineElementInternal aarGenerationTimelineElement = TimelineElementInternal.builder()
                .elementId("aarGenerationTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .details(sendDigitalDetailsIntSercq)
                .build();
        //PN riceve feedback positivo da External Channels per uno dei destinatari
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();
        TimelineElementInternal digitalDeliveryRequestFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("digitalDeliveryRequestFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST)
                .build();
        TimelineElementInternal pecReceivedFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("pecReceivedFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-15T15:32:00.00Z")))
                .category(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW)
                .build();

        // Il primo destinatario (vivo) visualizza la notifica.
        TimelineElementInternal firstRecViewedTimelineElement = TimelineElementInternal.builder()
                .elementId("firstRecViewedTimelineElement")
                .timestamp((Instant.parse("2021-09-15T18:00:00.00Z")))
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(0)
                        .eventTimestamp(Instant.parse("2021-09-15T18:00:00.00Z"))
                        .build())
                .build();

        //PN riceve feedback positivo da External Channels ma con deliveryFailureCause M02 (DECEDUTO) per l'altro destinatario
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-16T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        Instant deacesedSendAnalogFeedbackBusinessDate = Instant.parse("2021-09-17T06:40:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-17T15:40:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-18T15:45:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).build())
                .build();




        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendPecFirstRecipientTimelineElement, aarGenerationTimelineElement,
                feedbackOKFirstRecipientTimelineElement, digitalDeliveryRequestFirstRecipientTimelineElement,
                pecReceivedFirstRecipientTimelineElement, sendAnalogSecondRecipientTimelineElement, feedbackAnalogSecondRecipientTimelineElement,
                deceasedWorkflowSecondRecipientTimelineElement, firstRecViewedTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

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
                        .relatedTimelineElements(List.of("sendPecFirstRecipientTimelineElement", "aarGenerationTimelineElement",
                                "feedbackOKFirstRecipientTimelineElement", "digitalDeliveryRequestFirstRecipientTimelineElement",
                                "pecReceivedFirstRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.VIEWED)
                        .activeFrom(firstRecViewedTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of(
                                "firstRecViewedTimelineElement", "sendAnalogSecondRecipientTimelineElement",
                                "feedbackAnalogSecondRecipientTimelineElement", "deceasedWorkflowSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "5th status wrong"
        );
    }

    /*
        IN_VALIDATION - ACCEPTED - DELIVERING - EFFECTIVE_DATE
        un destinatario è raggiungibile via domicilio analogico e la notifica viene perfezionata per decorrenza termini.
        Per l'altro successivamente viene ricevuta una comunicazione di decesso.
        Ci aspettiamo che il perfezionamento avvenuto in precedenza
        non permetta il passaggio in DELIVERED, poichè EFFECTIVE_DATE è uno stato più avanzato.
        Stato finale: EFFECTIVE_DATE
     */
    @Test
    void getTimelineHistoryMultiRecipientWithOneDigitalWorkflowAndARefinementBeforeDeceasedWorkflowTest() {
        final int NUMBER_OF_RECIPIENTS = 2;

        SendDigitalDetailsInt sendDigitalDetailsIntPec = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        SendDigitalDetailsInt sendDigitalDetailsIntSercq = getSendDigitalDetails(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ);


        // GIVEN a timeline
        TimelineElementInternal requestAcceptedTimelineElement = TimelineElementInternal.builder()
                .elementId("requestAcceptedTimelineElement")
                .timestamp(Instant.parse("2021-09-10T15:24:00.00Z"))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();
        TimelineElementInternal sendAnalogFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:26:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(sendDigitalDetailsIntPec)
                .build();
        TimelineElementInternal sendAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("sendAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-11T15:28:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .build();
        TimelineElementInternal aarGenerationTimelineElement = TimelineElementInternal.builder()
                .elementId("aarGenerationTimelineElement")
                .timestamp((Instant.parse("2021-09-12T15:26:30.00Z")))
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .details(sendDigitalDetailsIntSercq)
                .build();
        //PN riceve feedback positivo da External Channels per il primo destinatario
        TimelineElementInternal feedbackOKFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackOKFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T15:30:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(Instant.parse("2021-09-13T15:00:00.00Z")).build())
                .build();
        TimelineElementInternal analogSuccessWorkflowFirstRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("analogSuccessWorkflowFirstRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-13T17:30:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW)
                .details(AnalogSuccessWorkflowDetailsInt.builder().recIndex(0).build())
                .build();

        // Viene effettuato il perfezionamento per decorrenza termini del primo destinatario.
        Instant firstRecScheduleRefinementBusinessDate = Instant.parse("2021-09-15T18:00:00.00Z");
        TimelineElementInternal firstRecScheduleRefinementTimelineElement = TimelineElementInternal.builder()
                .elementId("firstRecScheduleRefinementTimelineElement")
                .timestamp((Instant.parse("2021-09-15T18:00:00.00Z")))
                .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                .details(ScheduleRefinementDetailsInt.builder()
                        .recIndex(0)
                        .schedulingDate(firstRecScheduleRefinementBusinessDate)
                        .build())
                .build();

        TimelineElementInternal firstRecRefinementTimelineElement = TimelineElementInternal.builder()
                .elementId("firstRecRefinementTimelineElement")
                .timestamp((Instant.parse("2021-09-20T18:02:00.00Z")))
                .category(TimelineElementCategoryInt.REFINEMENT)
                .details(RefinementDetailsInt.builder()
                        .recIndex(0)
                        .eventTimestamp(Instant.parse("2021-09-20T18:02:00.00Z"))
                        .build())
                .build();

        //PN riceve feedback positivo da External Channels ma con deliveryFailureCause M02 (DECEDUTO) per l'altro destinatario
        Instant deacesedSendAnalogFeedbackBusinessDate = Instant.parse("2021-09-20T19:00:00.00Z");
        TimelineElementInternal feedbackAnalogSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("feedbackAnalogSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-21T19:00:00.00Z")))
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).deliveryFailureCause("M02").build())
                .build();
        TimelineElementInternal deceasedWorkflowSecondRecipientTimelineElement = TimelineElementInternal.builder()
                .elementId("deceasedWorkflowSecondRecipientTimelineElement")
                .timestamp((Instant.parse("2021-09-20T19:05:00.00Z")))
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder().recIndex(1).notificationDate(deacesedSendAnalogFeedbackBusinessDate).build())
                .build();

        Set<TimelineElementInternal> timelineElementList = Set.of(requestAcceptedTimelineElement,
                sendAnalogFirstRecipientTimelineElement, aarGenerationTimelineElement,
                feedbackOKFirstRecipientTimelineElement, analogSuccessWorkflowFirstRecipientTimelineElement,
                sendAnalogSecondRecipientTimelineElement, firstRecScheduleRefinementTimelineElement,
                firstRecRefinementTimelineElement, feedbackAnalogSecondRecipientTimelineElement,
                deceasedWorkflowSecondRecipientTimelineElement);


        // WHEN ask for status history
        Instant notificationCreatedAt = Instant.parse("2021-09-16T15:20:00.00Z");

        List<NotificationStatusHistoryElementInt> actualStatusHistory = statusUtils.getStatusHistory(
                timelineElementList,
                NUMBER_OF_RECIPIENTS,
                notificationCreatedAt
        );

        printStatus(actualStatusHistory, new Object() {
        }.getClass().getEnclosingMethod().getName());

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
                        .activeFrom(sendAnalogFirstRecipientTimelineElement.getTimestamp())
                        .relatedTimelineElements(List.of("sendAnalogFirstRecipientTimelineElement", "sendAnalogSecondRecipientTimelineElement",
                                "aarGenerationTimelineElement", "feedbackOKFirstRecipientTimelineElement",
                                "analogSuccessWorkflowFirstRecipientTimelineElement", "firstRecScheduleRefinementTimelineElement"))
                        .build(),
                actualStatusHistory.get(2),
                "3rd status wrong"
        );

        //  ... 4th initial status
        Assertions.assertEquals(NotificationStatusHistoryElementInt.builder()
                        .status(NotificationStatusInt.EFFECTIVE_DATE)
                        .activeFrom(firstRecScheduleRefinementBusinessDate) // Per il perfezionamento viene presa la data di scheduling
                        .relatedTimelineElements(List.of(
                                "firstRecRefinementTimelineElement", "feedbackAnalogSecondRecipientTimelineElement",
                                "deceasedWorkflowSecondRecipientTimelineElement"))
                        .build(),
                actualStatusHistory.get(3),
                "5th status wrong"
        );
    }

    private void printStatus(List<NotificationStatusHistoryElementInt> notificationHistoryElements, String methodName) {
        System.out.print(methodName + " - ");
        notificationHistoryElements.stream()
                .map(NotificationStatusHistoryElementInt::getStatus)
                .forEach(notificationStatusInt -> System.out.print(notificationStatusInt + " "));
        System.out.println();
    }
}
