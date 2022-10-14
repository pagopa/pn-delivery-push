package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        Instant eventTimestamp = Instant.parse("2021-09-16T15:24:00.00Z");
        TimelineElementDetailsInt details = buildTimelineElementDetailsInt();

        TimelineElementInternal expected = buildTimelineElementInternal();
        TimelineElementInternal actual = timelineUtils.buildTimeline(notification, category, elementId, eventTimestamp, details);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void buildAcceptedRequestTimelineElement() {
        NotificationInt notification = buildNotification();
        TimelineElementInternal actual = timelineUtils.buildAcceptedRequestTimelineElement(notification, "001");

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_request_accepted", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildAvailabilitySourceTimelineElement() {
        NotificationInt notification = buildNotification();
        TimelineElementInternal actual = timelineUtils.buildAvailabilitySourceTimelineElement(1, notification, DigitalAddressSourceInt.PLATFORM, Boolean.FALSE, 1);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_get_address_1_source_PLATFORM_attempt_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildDigitalFeedbackTimelineElement() {
        NotificationInt notification = buildNotification();
        Instant eventTimestamp = Instant.parse("2021-09-16T15:24:00.00Z");
        LegalDigitalAddressInt legalDigitalAddressInt = LegalDigitalAddressInt.builder()
                .address("Via nuova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        TimelineElementInternal actual = timelineUtils.buildDigitalFeedbackTimelineElement(notification, ResponseStatusInt.OK, Collections.EMPTY_LIST, 1, 1, legalDigitalAddressInt, DigitalAddressSourceInt.GENERAL, DigitalMessageReferenceInt.builder().build(), eventTimestamp);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_send_digital_feedback_1_source_GENERAL_attempt_1", actual.getElementId()),
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
        TimelineElementInternal actual = timelineUtils.buildDigitalProgressFeedbackTimelineElement(notification,
                recIndex, sentAttemptMade, eventCode, shouldRetry, digitalAddressInt, digitalAddressSourceInt, digitalMessageReference, progressIndex, eventTimestamp);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_digital_delivering_progress_1_source_GENERAL_attempt_1_progidx_1", actual.getElementId()),
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

        TimelineElementInternal actual = timelineUtils.buildSendCourtesyMessageTimelineElement(
                recIndex, notification, address, sendDate, eventId
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
        String eventId = "001";
        Integer numberOfPages = 1;

        TimelineElementInternal actual = timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recIndex, notification, address, eventId, numberOfPages);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("001", actual.getElementId()),
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

        TimelineElementInternal actual = timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, addressSource, recIndex, notification, sentAttemptMade, eventId);
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
        boolean investigation = Boolean.FALSE;
        int sentAttemptMade = 1;
        String eventId = "001";
        Integer numberOfPages = 10;

        TimelineElementInternal actual = timelineUtils.buildSendAnalogNotificationTimelineElement(
                address, recIndex, notification, investigation, sentAttemptMade, eventId, numberOfPages
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("001", actual.getElementId()),
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

        TimelineElementInternal actual = timelineUtils.buildSuccessDigitalWorkflowTimelineElement(
                notification, recIndex, address, legalFactId
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_digital_success_workflow_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildFailureDigitalWorkflowTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        String legalFactId = "001";

        TimelineElementInternal actual = timelineUtils.buildFailureDigitalWorkflowTimelineElement(
                notification, recIndex, legalFactId
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_digital_failure_workflow_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildSuccessAnalogWorkflowTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        PhysicalAddressInt address = buildPhysicalAddressInt();
        List<LegalFactsIdInt> attachments = Collections.singletonList(LegalFactsIdInt.builder()
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .key("key")
                .build());

        TimelineElementInternal actual = timelineUtils.buildSuccessAnalogWorkflowTimelineElement(
                notification, recIndex, address, attachments
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_analog_success_workflow_1", actual.getElementId()),
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

        TimelineElementInternal actual = timelineUtils.buildFailureAnalogWorkflowTimelineElement(
                notification, recIndex, attachments
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_analog_failure_workflow_1", actual.getElementId()),
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
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(legalDigitalAddressInt)
                .physicalAddress(physicalAddressInt)
                .correlationId("001")
                .build();

        TimelineElementInternal actual = timelineUtils.buildPublicRegistryResponseCallTimelineElement(
                notification, recIndex, response
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("public_registry_response_001", actual.getElementId()),
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
                notification, recIndex, eventId, deliveryMode, contactPhase, sentAttemptMade
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
        List<LegalFactsIdInt> legalFactsListEntryIds = Collections.singletonList(LegalFactsIdInt.builder()
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .key("key")
                .build());
        PhysicalAddressInt newAddress = buildPhysicalAddressInt();
        List<String> errors = Collections.singletonList("error 001");
        SendAnalogDetailsInt sendPaperDetails = SendAnalogDetailsInt.builder().build();

        TimelineElementInternal actual = timelineUtils.buildAnalogFailureAttemptTimelineElement(
                notification, sentAttemptMade, legalFactsListEntryIds, newAddress, errors, sendPaperDetails
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_send_paper_feedback_0_attempt_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildNotificationViewedTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        String legalFactId = "001";
        Integer notificationCost = 100;
        String raddType = "test";
        String raddTransactionId = "002";
        Instant eventTimestamp = Instant.now();

        TimelineElementInternal actual = timelineUtils.buildNotificationViewedTimelineElement(
                notification, recIndex, legalFactId, notificationCost, raddType, raddTransactionId, eventTimestamp
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_notification_viewed_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildCompletelyUnreachableTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;

        TimelineElementInternal actual = timelineUtils.buildCompletelyUnreachableTimelineElement(notification, recIndex);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_completely_unreachable_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildScheduleDigitalWorkflowTimeline() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        DigitalAddressInfo lastAttemptInfo = DigitalAddressInfo.builder()
                .sentAttemptMade(1)
                .lastAttemptDate(Instant.now())
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("Via nuova")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .build();

        TimelineElementInternal actual = timelineUtils.buildScheduleDigitalWorkflowTimeline(
                notification, recIndex, lastAttemptInfo
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_schedule_digital_workflow_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildScheduleAnalogWorkflowTimeline() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;

        TimelineElementInternal actual = timelineUtils.buildScheduleAnalogWorkflowTimeline(notification, recIndex);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_schedule_analog_workflow_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildRefinementTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        Integer notificationCost = 100;

        TimelineElementInternal actual = timelineUtils.buildRefinementTimelineElement(
                notification, recIndex, notificationCost
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_refinement_1", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
    }

    @Test
    void buildScheduleRefinement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 100;

        TimelineElementInternal actual = timelineUtils.buildScheduleRefinement(notification, recIndex);

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals("Example_IUN_1234_Test_schedule_refinement_workflow_100", actual.getElementId()),
                () -> Assertions.assertEquals("TEST_PA_ID", actual.getPaId())
        );
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

    private TimelineElementInternal buildTimelineElementInternal() {
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
}