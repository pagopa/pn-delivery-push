package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalProgressDetailsInt;
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

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

class ExternalChannelUtilsTest {

    @Mock
    private TimelineService timelineService;

    @Mock
    private TimelineUtils timelineUtils;

    private ExternalChannelUtils channelUtils;

    @BeforeEach
    void setUp() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        channelUtils = new ExternalChannelUtils(timelineService, timelineUtils);
    }

    @Test
    void addSendDigitalNotificationToTimeline() {
        NotificationInt notification = buildNotification();
        LegalDigitalAddressInt digitalAddress = buildLegalDigitalAddressInt();
        TimelineElementInternal t1 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa1").timestamp(Instant.now().minusMillis(30000))
                .details(SendDigitalProgressDetailsInt.builder().build())
                .build();
        Mockito.when(timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, DigitalAddressSourceInt.GENERAL, 1, notification, 1, "001")).thenReturn(t1);

        channelUtils.addSendDigitalNotificationToTimeline(notification, digitalAddress, DigitalAddressSourceInt.GENERAL, 1, 1, "001");
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(t1, notification);
    }



    @Test
    void getExternalChannelNotificationTimelineElement() {
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineService.getTimelineElement("001", "002")).thenReturn(Optional.of(timelineElementInternal));

        TimelineElementInternal actual = channelUtils.getExternalChannelNotificationTimelineElement("001", "002");

        Assertions.assertEquals(timelineElementInternal, actual);
    }


    @Test
    void getAarKeyOk() {
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(SAFE_STORAGE_URL_PREFIX + "testKey")
                .numberOfPages(1)
                .build();
        
        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of( aarGenerationDetails ));

        String testIun = "testIun";
        int recIndex = 0;
        String aarKey = channelUtils.getAarKey(testIun, recIndex);

        Assertions.assertEquals(aarGenerationDetails.getGeneratedAarUrl().replace(SAFE_STORAGE_URL_PREFIX, ""), aarKey);
    }

    @Test
    void getAarKeyNull() {
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(null)
                .numberOfPages(0)
                .build();

        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of( aarGenerationDetails ));

        String testIun = "testIun";
        int recIndex = 0;
        String aarKey = channelUtils.getAarKey(testIun, recIndex);

        Assertions.assertNull(aarKey);
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