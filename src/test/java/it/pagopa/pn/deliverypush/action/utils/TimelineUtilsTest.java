package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.*;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST;

class TimelineUtilsTest {

    @Mock
    private InstantNowSupplier instantNowSupplier;

    @Mock
    private TimelineService timelineService;

    private TimelineUtils timelineUtils;

    @BeforeEach
    void setUp() {
        instantNowSupplier = Mockito.mock(InstantNowSupplier.class);
        timelineService = Mockito.mock(TimelineService.class);

        timelineUtils = new TimelineUtils(instantNowSupplier, timelineService);
    }

    @Test
    void buildTimeline() {

        NotificationViewedDetailsInt detailsInt = new NotificationViewedDetailsInt();
        detailsInt.setRecIndex(0);
        detailsInt.setNotificationCost(100);
        TimelineElementInternal actual = timelineUtils.buildTimeline(buildNotificationInt(), TimelineElementCategoryInt.REQUEST_ACCEPTED, "001", buildTimelineElementDetailsInt());
        Assertions.assertEquals("001", actual.getIun());
        Assertions.assertEquals("001", actual.getElementId());
        Assertions.assertEquals("pa_02", actual.getPaId());
        Assertions.assertEquals(TimelineElementCategoryInt.REQUEST_ACCEPTED, actual.getCategory());
        Assertions.assertEquals(detailsInt, actual.getDetails());
    }

    @Test
    void buildTimelineSecond() {
        NotificationInt notification = buildNotification();
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        String elementId = "001";
        TimelineElementDetailsInt details = buildTimelineElementDetailsInt();

        TimelineElementInternal expected = buildTimelineElementInternal(notification);
        TimelineElementInternal actual = timelineUtils.buildTimeline(notification, category, elementId, details);

        actual.setTimestamp(expected.getTimestamp());// unico campo che sarà diverso, essendo istant.now
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void buildAcceptedRequestTimelineElement() {
        NotificationInt notification = buildNotification();
        TimelineElementInternal actual = timelineUtils.buildAcceptedRequestTimelineElement(notification, "001");
        String timelineEventIdExpected = "REQUEST_ACCEPTED#IUN_Example_IUN_1234_Test".replace("#", TimelineEventIdBuilder.DELIMITER);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildAvailabilitySourceTimelineElement() {
        NotificationInt notification = buildNotification();
        TimelineElementInternal actual = timelineUtils.buildAvailabilitySourceTimelineElement(1, notification, DigitalAddressSourceInt.PLATFORM, Boolean.FALSE, 1);
        String timelineEventIdExpected = "GET_ADDRESS#IUN_Example_IUN_1234_Test#RECINDEX_1#SOURCE_PLATFORM#ATTEMPT_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildDigitalFeedbackTimelineElement() {
        NotificationInt notification = buildNotification();
        Instant eventTimestamp = Instant.parse("2021-09-16T15:24:00.00Z");
        String timelineEventIdExpected = "SEND_DIGITAL_FEEDBACK#IUN_Example_IUN_1234_Test#RECINDEX_1#SOURCE_GENERAL#REPEAT_false#ATTEMPT_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        LegalDigitalAddressInt legalDigitalAddressInt = LegalDigitalAddressInt.builder()
                .address("Via nuova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(1)
                .eventTimestamp(eventTimestamp)
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(legalDigitalAddressInt)
                .build();
        
        TimelineElementInternal actual = 
                timelineUtils.buildDigitalFeedbackTimelineElement(
                        "digital_domicile_timeline_id_0",
                        notification, 
                        ResponseStatusInt.OK,
                        1,
                        ExtChannelDigitalSentResponseInt.builder()
                                .eventCode(EventCodeInt.C003)
                                .build(),
                        digitalAddressFeedback,
                        false
                );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals(EventCodeInt.C003.getValue(), ((SendDigitalFeedbackDetailsInt)actual.getDetails()).getDeliveryDetailCode()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildDigitalProgressFeedbackTimelineElement() {
        NotificationInt notification = buildNotification();
        int recIndex = 1;
        int sentAttemptMade = 1;
        EventCodeInt eventCode = EventCodeInt.C001;
        boolean shouldRetry = Boolean.FALSE;
        LegalDigitalAddressInt digitalAddressInt = LegalDigitalAddressInt.builder()
                .address("Via nuova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        DigitalAddressSourceInt digitalAddressSourceInt = DigitalAddressSourceInt.GENERAL;
        DigitalMessageReferenceInt digitalMessageReference = DigitalMessageReferenceInt.builder().build();
        int progressIndex = 1;
        Instant eventTimestamp = Instant.parse("2021-09-16T15:24:00.00Z");
        String timelineEventIdExpected = "DIGITAL_PROG#IUN_Example_IUN_1234_Test#RECINDEX_1#SOURCE_GENERAL.REPEAT_false#ATTEMPT_1#IDX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(sentAttemptMade)
                .eventTimestamp(eventTimestamp)
                .digitalAddressSource(digitalAddressSourceInt)
                .digitalAddress(digitalAddressInt)
                .isFirstSendRetry(false)
                .relatedFeedbackTimelineId(null)
                .build();
        
        TimelineElementInternal actual = timelineUtils.buildDigitalProgressFeedbackTimelineElement(
                notification,
                recIndex,
                eventCode, 
                shouldRetry, 
                digitalMessageReference, 
                progressIndex,
                digitalAddressFeedback
        );
        
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSendCourtesyMessageTimelineElement() {
        Integer recIndex = 1;
        NotificationInt notification = buildNotification();
        CourtesyDigitalAddressInt address = CourtesyDigitalAddressInt.builder()
                .address("test@works.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build();
        Instant sendDate = Instant.parse("2021-09-16T15:24:00.00Z");
        String eventId = "eventID001";
        IoSendMessageResultInt ioSendMessageResultInt = IoSendMessageResultInt.SENT_OPTIN;
        
        TimelineElementInternal actual = timelineUtils.buildSendCourtesyMessageTimelineElement(
                recIndex, notification, address, sendDate, eventId, ioSendMessageResultInt
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("eventID001", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSendSimpleRegisteredLetterTimelineElement() {
        Integer recIndex = 1;
        NotificationInt notification = buildNotification();
        PhysicalAddressInt address = buildPhysicalAddressInt();
        SendResponse sendResponse = new SendResponse()
                .amount(10);

        String productType ="RN_AR";
        String timelineEventIdExpected = "SEND_SIMPLE_REGISTERED_LETTER#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        TimelineElementInternal actual = timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recIndex, notification, address, sendResponse, productType, "request_id", null);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSendDigitalNotificationTimelineElement() {
        LegalDigitalAddressInt digitalAddress = LegalDigitalAddressInt.builder()
                .address("Via nuova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.GENERAL;
        Integer recIndex = 1;
        NotificationInt notification = buildNotification();
        int sentAttemptMade = 1;
        String eventId = "001";

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalAddress)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(false)
                .relatedFeedbackTimelineId(null)
                .build();
        
        TimelineElementInternal actual = timelineUtils.buildSendDigitalNotificationTimelineElement(recIndex, notification, sendInformation, eventId);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("001", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSendAnalogNotificationTimelineElement() {
        PhysicalAddressInt address = buildPhysicalAddressInt();
        Integer recIndex = 1;
        NotificationInt notification = buildNotification();
        String relatedRequestId = null;
        int sentAttemptMade = 1;
        SendResponse sendResponse = new SendResponse()
                .amount(10);

        String productType ="RN_AR";
        String timelineEventIdExpected = "SEND_ANALOG_DOMICILE#IUN_Example_IUN_1234_Test#RECINDEX_1#ATTEMPT_1".replace("#", TimelineEventIdBuilder.DELIMITER);


        AnalogDtoInt analogDtoInfo = AnalogDtoInt.builder()
                .sentAttemptMade(sentAttemptMade)
                .sendResponse(sendResponse)
                .relatedRequestId(relatedRequestId)
                .productType(productType)
                .prepareRequestId("prepare_request_id")
                .build();
        
        TimelineElementInternal actual = timelineUtils.buildSendAnalogNotificationTimelineElement(
                address, recIndex, notification, analogDtoInfo, List.of("replacedF24AttachmentUrls")
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSuccessDigitalWorkflowTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        LegalDigitalAddressInt address = LegalDigitalAddressInt.builder()
                .address("Via nuova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        String legalFactId = "001";
        String timelineEventIdExpected = "DIGITAL_SUCCESS_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        TimelineElementInternal actual = timelineUtils.buildSuccessDigitalWorkflowTimelineElement(
                notification, recIndex, address, legalFactId
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildFailureDigitalWorkflowTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        String legalFactId = "001";
        String timelineEventIdExpected = "DIGITAL_FAILURE_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        TimelineElementInternal actual = timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSuccessAnalogWorkflowTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        PhysicalAddressInt address = buildPhysicalAddressInt();
        String timelineEventIdExpected = "ANALOG_SUCCESS_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        TimelineElementInternal actual = timelineUtils.buildSuccessAnalogWorkflowTimelineElement(
                notification, recIndex, address
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildFailureAnalogWorkflowTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        List<LegalFactsIdInt> attachments = Collections.singletonList(LegalFactsIdInt.builder()
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .key("key")
                .build());
        String timelineEventIdExpected = "ANALOG_FAILURE_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        TimelineElementInternal actual = timelineUtils.buildFailureAnalogWorkflowTimelineElement(
                notification, recIndex, "aarUrl"
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildPublicRegistryResponseCallTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        LegalDigitalAddressInt legalDigitalAddressInt = LegalDigitalAddressInt.builder()
                .address("Via nuova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        PhysicalAddressInt physicalAddressInt = buildPhysicalAddressInt();
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(legalDigitalAddressInt)
                .physicalAddress(physicalAddressInt)
                .correlationId("001")
                .build();

        String timelineEventIdExpected = "NATIONAL_REGISTRY_RESPONSE#CORRELATIONID_001".replace("#", TimelineEventIdBuilder.DELIMITER);

        TimelineElementInternal actual = timelineUtils.buildPublicRegistryResponseCallTimelineElement(
                notification, recIndex, response
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildPublicRegistryCallTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        String eventId = "001";
        DeliveryModeInt deliveryMode = DeliveryModeInt.DIGITAL;
        ContactPhaseInt contactPhase = ContactPhaseInt.CHOOSE_DELIVERY;
        int sentAttemptMade = 1;

        TimelineElementInternal actual = timelineUtils.buildPublicRegistryCallTimelineElement(
                notification, recIndex, eventId, deliveryMode, contactPhase, sentAttemptMade, null
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("001", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildAnalogFailureAttemptTimelineElement() {
        NotificationInt notification = buildNotification();
        int sentAttemptMade = 1;
        List<AttachmentDetailsInt> attachments = new ArrayList<>();
        attachments.add(AttachmentDetailsInt.builder().url("key").build());
        
        SendAnalogDetailsInt sendPaperDetails = SendAnalogDetailsInt.builder().build();
        
        SendEventInt sendEventInt = SendEventInt.builder()
                .statusDateTime(Instant.now())
                .statusCode("KO")
                .statusDetail("ABCD")
                .deliveryFailureCause("M1")
                .build();

        final String sendRequestId = "send_request_id";

        TimelineElementInternal actual = timelineUtils.buildAnalogFailureAttemptTimelineElement(
                notification, sentAttemptMade, attachments, sendPaperDetails, sendEventInt, sendRequestId
        );

        String timelineEventIdExpected = "SEND_ANALOG_FEEDBACK#IUN_Example_IUN_1234_Test#RECINDEX_0#ATTEMPT_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildNotificationViewedTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        String legalFactId = "001";
        Integer notificationCost = 100;
        RaddInfo raddInfo = RaddInfo.builder()
                .type("test")
                .transactionId("002")
                .build();
        DelegateInfoInt delegateInfoInt = DelegateInfoInt.builder()
                .delegateType(RecipientTypeInt.PF)
                .mandateId("mandate")
                .operatorUuid("iioaxx11")
                .internalId("internCF")
                .build();
        
        Instant eventTimestamp = Instant.now();

        TimelineElementInternal actual = timelineUtils.buildNotificationViewedTimelineElement(
                notification, recIndex, legalFactId, notificationCost, raddInfo, delegateInfoInt, eventTimestamp
        );

        String timelineEventIdExpected = "NOTIFICATION_VIEWED#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildCompletelyUnreachableTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;

        TimelineElementInternal actual = timelineUtils.buildCompletelyUnreachableTimelineElement(notification, recIndex, "legal1", Instant.now());
        String timelineEventIdExpected = "COMPLETELY_UNREACHABLE#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildScheduleDigitalWorkflowTimeline() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        DigitalAddressInfoSentAttempt lastAttemptInfo = DigitalAddressInfoSentAttempt.builder()
                .sentAttemptMade(1)
                .lastAttemptDate(Instant.now())
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("Via nuova")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .build();

        TimelineElementInternal actual = timelineUtils.buildScheduleDigitalWorkflowTimeline(
                notification, recIndex, lastAttemptInfo, Instant.EPOCH.plusMillis(10)
        );

        String timelineEventIdExpected = "SCHEDULE_DIGITAL_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_1#SOURCE_GENERAL#ATTEMPT_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId()),
                () -> Assertions.assertEquals(Instant.EPOCH.plusMillis(10), ((ScheduleDigitalWorkflowDetailsInt)actual.getDetails()).getSchedulingDate())
        );
    }

    @Test
    void buildScheduleAnalogWorkflowTimeline() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;

        TimelineElementInternal actual = timelineUtils.buildScheduleAnalogWorkflowTimeline(notification, recIndex, Instant.EPOCH.plusMillis(10));
        String timelineEventIdExpected = "SCHEDULE_ANALOG_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId()),
                () -> Assertions.assertEquals(Instant.EPOCH.plusMillis(10), ((ScheduleAnalogWorkflowDetailsInt)actual.getDetails()).getSchedulingDate())
        );
    }


    @Test
    void buildPrepareAnalogFailureTimelineElement() {
        NotificationInt notification = buildNotification();
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        Integer recIndex = 1;

        TimelineElementInternal actual = timelineUtils.buildPrepareAnalogFailureTimelineElement(addressInt, "prepare_id", "D01", recIndex, notification);
        String timelineEventIdExpected = "PREPARE_ANALOG_DOMICILE_FAILURE#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildRefinementTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        Integer notificationCost = 100;
        Instant refDate = Instant.EPOCH.plusMillis(10);

        TimelineElementInternal actual = timelineUtils.buildRefinementTimelineElement(
                notification, recIndex, notificationCost,true, refDate
        );
        String timelineEventIdExpected = "REFINEMENT#IUN_Example_IUN_1234_Test#RECINDEX_1".replace("#", TimelineEventIdBuilder.DELIMITER);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId()),
                () -> Assertions.assertEquals(Instant.EPOCH.plusMillis(10), ((RefinementDetailsInt)actual.getDetails()).getEventTimestamp())
        );
    }

    @Test
    void buildScheduleRefinement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 100;

        TimelineElementInternal actual = timelineUtils.buildScheduleRefinement(notification, recIndex, Instant.now());
        String timelineEventIdExpected = "SCHEDULE_REFINEMENT_WORKFLOW#IUN_Example_IUN_1234_Test#RECINDEX_100".replace("#", TimelineEventIdBuilder.DELIMITER);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void checkNotificationIsAlreadyViewedWithCreationRequest() {
        String iun = "testIun";
        Integer recIndex = 0;

        String creationRequestTimelineId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        
        Mockito.when(timelineService.getTimelineElement(iun, creationRequestTimelineId)).thenReturn(Optional.of(TimelineElementInternal.builder().build()));
        
        boolean notificationIsAlreadyViewed = timelineUtils.checkIsNotificationViewed(iun, recIndex);
        
        Assertions.assertTrue(notificationIsAlreadyViewed);
    }

    @Test
    void checkNotificationIsAlreadyViewedWithNotificationView() {
        String iun = "testIun";
        Integer recIndex = 0;

        String creationRequestTimelineId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, creationRequestTimelineId)).thenReturn(Optional.empty());

        String notificationViewedTimelineId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, notificationViewedTimelineId)).thenReturn(Optional.of(TimelineElementInternal.builder().build()));

        boolean notificationIsAlreadyViewed = timelineUtils.checkIsNotificationViewed(iun, recIndex);

        Assertions.assertTrue(notificationIsAlreadyViewed);
    }

    @Test
    void checkNotificationIsNotViewed() {
        String iun = "testIun";
        Integer recIndex = 0;

        String creationRequestTimelineId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, creationRequestTimelineId)).thenReturn(Optional.empty());

        String notificationViewedTimelineId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, notificationViewedTimelineId)).thenReturn(Optional.empty());

        boolean notificationIsAlreadyViewed = timelineUtils.checkIsNotificationViewed(iun, recIndex);

        Assertions.assertFalse(notificationIsAlreadyViewed);
    }

    @Test
    void checkIsNotificationNotPaidNull() {
        String iun = "testIun";
        Integer recIndex = 0;

        Set<TimelineElementInternal> setTimelineElement = null;
        Mockito.when(timelineService.getTimelineByIunTimelineId(Mockito.eq(iun), Mockito.anyString(), Mockito.eq(false))).thenReturn(setTimelineElement);
        
        boolean isNotificationPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
        Assertions.assertFalse(isNotificationPaid);
    }

    @Test
    void checkIsNotificationNotPaid() {
        String iun = "testIun";
        Integer recIndex = 0;

        Mockito.when(timelineService.getTimelineByIunTimelineId(Mockito.eq(iun), Mockito.anyString(), Mockito.eq(false))).thenReturn(new HashSet<>());

        boolean isNotificationPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
        Assertions.assertFalse(isNotificationPaid);
    }

    @Test
    void checkIsNotificationNotPaidWithElements() {
        String iun = "testIun";
        Integer recIndex = 0;

        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW)
                .elementId("test")
                .build();
        setTimelineElement.add(timelineElementInternal);
        
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, "test1", false)).thenReturn(setTimelineElement);

        boolean isNotificationPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
        Assertions.assertFalse(isNotificationPaid);
    }

    @Test
    void checkIsNotificationPaid() {
        String iun = "testIun";
        Integer recIndex = 0;

        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();

        String timelineEventId = TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PAYMENT)
                .elementId(timelineEventId)
                .details(NotificationPaidDetailsInt.builder()
                        .recIndex(recIndex)
                        .build())
                .build();
        
        setTimelineElement.add(timelineElementInternal);
        
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, timelineEventId, false)).thenReturn(setTimelineElement);

        boolean isNotificationPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
        Assertions.assertTrue(isNotificationPaid);
    }

    @Test
    void checkIsNotificationPaidDifferentRecipient() {
        String iun = "testIun";
        Integer recIndex = 1;

        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();

        String timelineEventId = TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PAYMENT)
                .elementId(timelineEventId)
                .details(NotificationPaidDetailsInt.builder()
                        .recIndex(0)
                        .build())
                .build();

        TimelineElementInternal timelineElementInternal2 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .elementId("timelineEventId2")
                .details(AarGenerationDetailsInt.builder()
                        .recIndex(recIndex)
                        .build())
                .build();
        
        setTimelineElement.add(timelineElementInternal);
        setTimelineElement.add(timelineElementInternal2);
        
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, timelineEventId, false)).thenReturn(setTimelineElement);

        boolean isNotificationPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
        Assertions.assertFalse(isNotificationPaid);
    }

    @Test
    void checkIsNotificationPaidSameRecipient() {
        String iun = "testIun";
        Integer recIndex = 0;

        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();

        String timelineEventId = TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PAYMENT)
                .elementId(timelineEventId)
                .details(NotificationPaidDetailsInt.builder()
                        .recIndex(recIndex)
                        .build())
                .build();

        TimelineElementInternal timelineElementInternal2 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .elementId("timelineEventId2")
                .details(AarGenerationDetailsInt.builder()
                        .recIndex(recIndex)
                        .build())
                .build();

        setTimelineElement.add(timelineElementInternal);
        setTimelineElement.add(timelineElementInternal2);

        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, timelineEventId, false)).thenReturn(setTimelineElement);

        boolean isNotificationPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
        Assertions.assertTrue(isNotificationPaid);
    }

    @Test
    void checkNotificationIsViewedOrRefinedOrCancelled_NoOne() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;

        String viewedElementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, viewedElementId)).thenReturn(Optional.empty());


        String refinedElementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        Mockito.when(timelineService.getTimelineElement(iun, refinedElementId)).thenReturn(Optional.empty());

        String cancelledElementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, cancelledElementId, false))
                .thenReturn(Collections.emptySet());
        
        //WHEN
        boolean viewedOrRefinedOrCancelled = timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, recIndex);
        
        //THEN
        Assertions.assertFalse(viewedOrRefinedOrCancelled);
    }

    @Test
    void checkNotificationIsViewedOrRefinedOrCancelled_Viewed() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;

        String viewedElementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, viewedElementId)).thenReturn(Optional.of(TimelineElementInternal.builder().build()));


        String refinedElementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        Mockito.when(timelineService.getTimelineElement(iun, refinedElementId)).thenReturn(Optional.empty());

        String cancelledElementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, cancelledElementId, false))
                .thenReturn(Collections.emptySet());

        //WHEN
        boolean viewedOrRefinedOrCancelled = timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, recIndex);

        //THEN
        Assertions.assertTrue(viewedOrRefinedOrCancelled);
    }

    @Test
    void checkNotificationIsViewedOrRefinedOrCancelled_Refined() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;

        String viewedElementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, viewedElementId)).thenReturn(Optional.empty() );


        String refinedElementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        Mockito.when(timelineService.getTimelineElement(iun, refinedElementId)).thenReturn(Optional.of(TimelineElementInternal.builder().build()));

        String cancelledElementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, cancelledElementId, false))
                .thenReturn(Collections.emptySet());

        //WHEN
        boolean viewedOrRefinedOrCancelled = timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, recIndex);

        //THEN
        Assertions.assertTrue(viewedOrRefinedOrCancelled);
    }

    @Test
    void checkNotificationIsViewedOrRefinedOrCancelled_Cancelled() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;

        String viewedElementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Mockito.when(timelineService.getTimelineElement(iun, viewedElementId)).thenReturn(Optional.empty() );


        String refinedElementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        Mockito.when(timelineService.getTimelineElement(iun, refinedElementId)).thenReturn(Optional.empty() );

        String cancelledElementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, cancelledElementId, false))
                .thenReturn(Collections.singleton(TimelineElementInternal.builder().build()));

        //WHEN
        boolean viewedOrRefinedOrCancelled = timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, recIndex);

        //THEN
        Assertions.assertTrue(viewedOrRefinedOrCancelled);
    }
    
    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }

    private NotificationInt buildNotification() {
        return NotificationInt.builder()
                .sender(createSender())
                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                .iun("Example_IUN_1234_Test")
                .subject("notification test subject")
                .documents(Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(buildRecipients())
                .build();
    }

    private List<NotificationRecipientInt> buildRecipients() {
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .internalId("internalId")
                .taxId("CDCFSC11R99X001Z")
                .denomination("Galileo Bruno")
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(new PhysicalAddressInt(
                        "Galileo Bruno",
                        "Palazzo dell'Inquisizione",
                        "corso Italia 666",
                        "Piano Terra (piatta)",
                        "00100",
                        "Roma",
                        null,
                        "RM",
                        "IT"
                ))
                .build();

        return Collections.singletonList(rec1);
    }

    private PhysicalAddressInt buildPhysicalAddressInt() {
        return PhysicalAddressInt.builder()
                .addressDetails("001")
                .foreignState("002")
                .at("003")
                .province("004")
                .municipality("005")
                .zip("006")
                .municipalityDetails("007")
                .build();
    }

    private TimelineElementInternal buildTimelineElementInternal(NotificationInt notification) {
        Instant eventTimestamp = Instant.parse("2021-09-16T15:24:00.00Z");
        NotificationViewedDetailsInt notificationViewedDetailsInt = buildNotificationViewedDetailsInt();
        return TimelineElementInternal.builder()
                .elementId("001")
                .iun("Example_IUN_1234_Test")
                .timestamp(eventTimestamp)
                .paId("TEST_PA_ID")
                .legalFactsIds(Collections.EMPTY_LIST)
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .details(notificationViewedDetailsInt)
                .notificationSentAt(notification.getSentAt())
                .build();
    }

    private NotificationViewedDetailsInt buildNotificationViewedDetailsInt() {
        return NotificationViewedDetailsInt.builder()
                .recIndex(0)
                .notificationCost(100)
                .build();
    }


    private TimelineElementDetailsInt parseDetailsFromEntity(TimelineElementDetailsEntity entity, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(entity, category.getDetailsJavaClass());
    }

    private TimelineElementDetailsInt buildTimelineElementDetailsInt() {
        return parseDetailsFromEntity(TimelineElementDetailsEntity.builder()
                .recIndex(0)
                .notificationCost(100)
                .build(), TimelineElementCategoryInt.NOTIFICATION_VIEWED);

    }

    private NotificationInt buildNotificationInt() {
        return NotificationInt.builder()
                .iun("001")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId("pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }

    @Test
    void checkIsNotificationCancellationNotRequested() {
        String iun = "IUN-checkIsNotificationCancellationNotRequested";

        Mockito.when(timelineService.getTimelineByIunTimelineId(Mockito.eq(iun), Mockito.anyString(), Mockito.eq(false))).thenReturn(new HashSet<>());

        boolean isNotificationCancellationRequested = timelineUtils.checkIsNotificationCancellationRequested (iun);
        Assertions.assertFalse(isNotificationCancellationRequested);
    }

    @Test
    void checkIsNotificationCancellationRequested() {
        String iun = "IUN-checkIsNotificationCancellationRequested";

        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();

        String timelineEventId = TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
            EventId.builder()
                .iun(iun)
                .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST)
            .elementId(timelineEventId)
            .details(NotificationPaidDetailsInt.builder()
                .build())
            .build();

        setTimelineElement.add(timelineElementInternal);

        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, timelineEventId, false)).thenReturn(setTimelineElement);

        boolean isNotificationCancellationRequested = timelineUtils.checkIsNotificationCancellationRequested(iun);
        Assertions.assertTrue(isNotificationCancellationRequested);
    }

    @Test
    void checkIsNotificationRefined() {
        String iun = "IUN-checkIsNotificationRefined";

        Mockito.when(timelineService.getTimelineElement(Mockito.eq(iun), Mockito.anyString())).thenReturn(Optional.of(TimelineElementInternal.builder().build()));

        boolean isNotificationRefined = timelineUtils.checkIsNotificationRefined (iun, 0);
        Assertions.assertTrue(isNotificationRefined);
    }

    @Test
    void checkIsNotificationRefinedFalse() {
        String iun = "IUN-checkIsNotificationRefinedFalse";

        Mockito.when(timelineService.getTimelineElement(Mockito.eq(iun), Mockito.anyString())).thenReturn(Optional.empty());

        boolean isNotificationRefined = timelineUtils.checkIsNotificationRefined (iun, 0);
        Assertions.assertFalse(isNotificationRefined);
    }

    @Test
    void buildCancelRequestTimelineElement() {
        NotificationInt notification = buildNotification();

        String timelineEventIdExpected = "NOTIFICATION_CANCELLATION_REQUEST.IUN_Example_IUN_1234_Test";

        TimelineElementInternal actual = timelineUtils.buildCancelRequestTimelineElement(
            notification
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
            () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
            () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildCancelledTimelineElement() {
        NotificationInt notification = buildNotification();

        String timelineEventIdExpected = "NOTIFICATION_CANCELLED.IUN_Example_IUN_1234_Test";

        TimelineElementInternal actual = timelineUtils.buildCancelledTimelineElement(
            notification
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
            () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
            () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );

        NotificationCancelledDetailsInt detailsInt = (NotificationCancelledDetailsInt) actual.getDetails();
        Assertions.assertEquals(100, detailsInt.getNotificationCost());
        Assertions.assertEquals(1, detailsInt.getNotRefinedRecipientIndexes().size());
    }
    @Test
    void buildCancelledTimelineElementPerfectionated() {
        NotificationInt notification = buildNotification();
        NotificationViewedDetailsInt notificationViewedDetailsInt = NotificationViewedDetailsInt.builder().notificationCost(1).build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().
            elementId(TimelineEventId.NOTIFICATION_VIEWED.getValue()).
            details(notificationViewedDetailsInt).
            build();
        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineElementInternal));

        String timelineEventIdExpected = "NOTIFICATION_CANCELLED.IUN_Example_IUN_1234_Test";

        TimelineElementInternal actual = timelineUtils.buildCancelledTimelineElement(
            notification
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
            () -> Assertions.assertEquals(timelineEventIdExpected, actual.getElementId()),
            () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );

        NotificationCancelledDetailsInt detailsInt = (NotificationCancelledDetailsInt) actual.getDetails();
        Assertions.assertEquals(0, detailsInt.getNotificationCost());
        Assertions.assertEquals(0, detailsInt.getNotRefinedRecipientIndexes().size());
    }
}