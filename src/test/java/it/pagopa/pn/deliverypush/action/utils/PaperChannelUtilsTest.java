package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

class PaperChannelUtilsTest {

    @Mock
    private TimelineService timelineService;

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private PaperChannelUtils channelUtils;

    @BeforeEach
    void setUp() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        channelUtils = new PaperChannelUtils(timelineService, timelineUtils, pnDeliveryPushConfigs);
    }


    @Test
    void getSenderAddress() {
        // GIVEN

        PnDeliveryPushConfigs.PaperChannel externalChannel = new PnDeliveryPushConfigs.PaperChannel();
        PnDeliveryPushConfigs.SenderAddress senderAddress = new PnDeliveryPushConfigs.SenderAddress();
        senderAddress.setAddress("via casa");
        senderAddress.setFullname("pagoa");
        senderAddress.setCity("citta");
        externalChannel.setSenderAddress(senderAddress);

        Mockito.when(pnDeliveryPushConfigs.getPaperChannel()).thenReturn(externalChannel);

        // WHEN
        PhysicalAddressInt physicalAddressInt = channelUtils.getSenderAddress();

        // THEN
        Assertions.assertNotNull(physicalAddressInt);
        Assertions.assertNotNull(physicalAddressInt.getAddress());
        Assertions.assertNotNull(physicalAddressInt.getFullname());
        Assertions.assertNotNull(physicalAddressInt.getMunicipality());
    }

    @Test
    void addSendSimpleRegisteredLetterToTimeline() {
        NotificationInt notification = buildNotification();
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        SendResponse sendResponse = new SendResponse()
                .amount(10);
        Mockito.when(timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(1, notification, addressInt,  sendResponse, "RN_AR", "request_id")).thenReturn(timelineElementInternal);
        channelUtils.addSendSimpleRegisteredLetterToTimeline(notification, addressInt, 1, sendResponse, "RN_AR", "request_id");
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }


    @Test
    void addSendAnalogNotificationToTimeline() {
        NotificationInt notification = buildNotification();
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        SendResponse sendResponse = new SendResponse()
                .amount(10);
        
        AnalogDtoInt analogDtoInfo = AnalogDtoInt.builder()
                .sentAttemptMade(0)
                .sendResponse(sendResponse)
                .relatedRequestId(null)
                .productType("NR_AR")
                .prepareRequestId("prepare_request_id")
                .build();

        Mockito.when(timelineUtils.buildSendAnalogNotificationTimelineElement(addressInt, 1, notification,analogDtoInfo)).thenReturn(timelineElementInternal);
        
        channelUtils.addSendAnalogNotificationToTimeline(notification, addressInt, 1,   analogDtoInfo);
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }

    @Test
    void addPaperNotificationNotHandledToTimeline() {
        NotificationInt notification = buildNotification();
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineUtils.buildNotHandledTimelineElement(notification, 1, NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_CODE, NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_REASON)).thenReturn(timelineElementInternal);
        channelUtils.addPaperNotificationNotHandledToTimeline(notification, 1);
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }


    @Test
    void addPrepareAnalogFailureTimelineElement() {
        NotificationInt notification = buildNotification();
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineUtils.buildPrepareAnalogFailureTimelineElement(addressInt, "prep_id", "D01", 1, notification)).thenReturn(timelineElementInternal);
        channelUtils.addPrepareAnalogFailureTimelineElement(addressInt, "prep_id", "D01",1,  notification);
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }

    @Test
    void getPaperChannelNotificationTimelineElement() {
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineService.getTimelineElement("001", "002")).thenReturn(Optional.of(timelineElementInternal));

        TimelineElementInternal actual = channelUtils.getPaperChannelNotificationTimelineElement("001", "002");

        Assertions.assertEquals(timelineElementInternal, actual);
    }

    @Test
    void getSendRequestId() {
        String iun = "001";

        final String prepareRequestIdAnalog = "prepare_request_id_analog";
        
        SendAnalogDetailsInt detailsInt = SendAnalogDetailsInt.builder()
                .prepareRequestId(prepareRequestIdAnalog)
                .build();
        final TimelineElementInternal sendAnalog = TimelineElementInternal.builder()
                .iun("1")
                .elementId("SEND_ANALOG_DETAILS")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(detailsInt)
                .build();

        SendAnalogFeedbackDetailsInt feedbackDetails = SendAnalogFeedbackDetailsInt.builder()
                .sendRequestId(sendAnalog.getElementId())
                .build();
        final TimelineElementInternal sendAnalogFeedback = TimelineElementInternal.builder()
                .iun("1")
                .elementId("SEND_ANALOG_FEEDBACK")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(feedbackDetails)
                .build();

        final String prepareRequestIdSimpleRegisteredLetter = "prepare_request_id_simple_registered_letter";
        SimpleRegisteredLetterDetailsInt detailsSimpleRegisteredLetterInt = SimpleRegisteredLetterDetailsInt.builder()
                .prepareRequestId(prepareRequestIdSimpleRegisteredLetter)
                .build();
        final TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .iun("2")
                .elementId("SEND_SIMPLE_REGISTERED_LETTER_DETAILS")
                .timestamp(Instant.now())
                .paId("2")
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(detailsSimpleRegisteredLetterInt)
                .build();

        SimpleRegisteredLetterProgressDetailsInt progressDetails = SimpleRegisteredLetterProgressDetailsInt.builder()
                .sendRequestId(sendSimpleRegisteredLetter.getElementId())
                .build();
        final TimelineElementInternal sendSimpleRegisteredLetterProgress = TimelineElementInternal.builder()
                .iun("2")
                .elementId("SEND_SIMPLE_REGISTERED_LETTER_PROGRESS")
                .timestamp(Instant.now())
                .paId("2")
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS)
                .details(progressDetails)
                .build();

        
        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(sendAnalog);
        timeline.add(sendAnalogFeedback);
        timeline.add(sendSimpleRegisteredLetter);
        timeline.add(sendSimpleRegisteredLetterProgress);
        
        Mockito.when(timelineService.getTimeline(iun, false)).thenReturn(timeline);

        String sendRequestIdSendAnalog = channelUtils.getSendRequestIdByPrepareRequestId(iun, prepareRequestIdAnalog);
        String sendRequestIdSendSimpleLetter = channelUtils.getSendRequestIdByPrepareRequestId(iun, prepareRequestIdSimpleRegisteredLetter);

        Assertions.assertEquals(sendRequestIdSendAnalog, sendAnalog.getElementId());
        Assertions.assertEquals(sendRequestIdSendSimpleLetter, sendSimpleRegisteredLetter.getElementId());

    }

    @Test
    void getSendRequestIdNotFound() {
        String iun = "001";

        final String prepareRequestId = "prepare_request_id";

        SendAnalogDetailsInt detailsInt = SendAnalogDetailsInt.builder()
                .prepareRequestId(prepareRequestId + "different")
                .build();
        final TimelineElementInternal sendAnalog = TimelineElementInternal.builder()
                .iun("1")
                .elementId("SEND_ANALOG_DETAILS")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(detailsInt)
                .build();

        SendAnalogFeedbackDetailsInt feedbackDetails = SendAnalogFeedbackDetailsInt.builder()
                .sendRequestId(sendAnalog.getElementId())
                .build();
        final TimelineElementInternal sendAnalogFeedback = TimelineElementInternal.builder()
                .iun("1")
                .elementId("SEND_ANALOG_FEEDBACK")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(feedbackDetails)
                .build();


        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(sendAnalog);
        timeline.add(sendAnalogFeedback);

        Mockito.when(timelineService.getTimeline(iun, false)).thenReturn(timeline);

        String sendRequestId = channelUtils.getSendRequestIdByPrepareRequestId(iun, prepareRequestId);

        Assertions.assertNull(sendRequestId);
    }

    @Test
    void getSendRequestIdNotPresent() {
        String iun = "001";

        final String prepareRequestId = "prepare_request_id";
        
        SendAnalogFeedbackDetailsInt feedbackDetails = SendAnalogFeedbackDetailsInt.builder()
                .sendRequestId("test")
                .build();
        final TimelineElementInternal sendAnalogFeedback = TimelineElementInternal.builder()
                .iun("1")
                .elementId("SEND_ANALOG_FEEDBACK")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .details(feedbackDetails)
                .build();


        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(sendAnalogFeedback);

        Mockito.when(timelineService.getTimeline(iun, false)).thenReturn(timeline);

        String sendRequestId = channelUtils.getSendRequestIdByPrepareRequestId(iun, prepareRequestId);

        Assertions.assertNull(sendRequestId);
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

    private TimelineElementInternal buildTimelineElementInternal() {
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        NotificationInt notificationInt = buildNotification();

        SimpleRegisteredLetterDetailsInt details = SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(1)
                .physicalAddress(addressInt)
                .foreignState(addressInt.getForeignState())
                .build();

        return TimelineElementInternal.builder()
                .elementId("001")
                .iun(notificationInt.getIun())
                .details(details)
                .paId(notificationInt.getSender().getPaId())
                .build();
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

    private LegalDigitalAddressInt buildLegalDigitalAddressInt() {
        return LegalDigitalAddressInt.builder()
                .address("test@dominioPec.it")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
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

}