package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
import java.util.Optional;

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
                .amount(10)
                .foreignState("FR");
        Mockito.when(timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(1, notification, addressInt,  sendResponse, "RN_AR")).thenReturn(timelineElementInternal);
        channelUtils.addSendSimpleRegisteredLetterToTimeline(notification, addressInt, 1, sendResponse, "RN_AR");
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }


    @Test
    void addSendAnalogNotificationToTimeline() {
        NotificationInt notification = buildNotification();
        PhysicalAddressInt addressInt = buildPhysicalAddressInt();
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        SendResponse sendResponse = new SendResponse()
                .amount(10)
                .foreignState("FR");
        Mockito.when(timelineUtils.buildSendAnalogNotificationTimelineElement(addressInt, 1, notification, null, 0,  sendResponse, "NR_AR")).thenReturn(timelineElementInternal);
        channelUtils.addSendAnalogNotificationToTimeline(notification, addressInt, 1,   0, sendResponse, null, "NR_AR");
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
    void getPaperChannelNotificationTimelineElement() {
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineService.getTimelineElement("001", "002")).thenReturn(Optional.of(timelineElementInternal));

        TimelineElementInternal actual = channelUtils.getPaperChannelNotificationTimelineElement("001", "002");

        Assertions.assertEquals(timelineElementInternal, actual);
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