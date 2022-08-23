package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ChooseDeliveryModeUtilsTest {

    private TimelineService timelineService;
    private TimelineUtils timelineUtils;
    private CourtesyMessageUtils courtesyMessageUtils;
    private AddressBookService addressBookService;
    private NotificationUtils notificationUtils;
    private ChooseDeliveryModeUtils chooseDeliveryModeUtils;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        courtesyMessageUtils = Mockito.mock(CourtesyMessageUtils.class);
        addressBookService = Mockito.mock(AddressBookService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        chooseDeliveryModeUtils = new ChooseDeliveryModeUtils(timelineService, timelineUtils, courtesyMessageUtils, addressBookService, notificationUtils);
    }

    @Test
    void addAvailabilitySourceToTimeline() {

        Mockito.when(timelineUtils.buildAvailabilitySourceTimelineElement(Mockito.anyInt(), Mockito.any(NotificationInt.class), Mockito.any(DigitalAddressSourceInt.class), Mockito.anyBoolean(), Mockito.eq(0))).thenReturn(Mockito.any(TimelineElementInternal.class));

        chooseDeliveryModeUtils.addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class), Mockito.any(DigitalAddressSourceInt.class), Mockito.anyBoolean());

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void addScheduleAnalogWorkflowToTimeline() {

        NotificationInt notificationInt = buildNotification();
        
        Mockito.when(timelineUtils.buildScheduleAnalogWorkflowTimeline(notificationInt, 1)).thenReturn(Mockito.any(TimelineElementInternal.class));
        chooseDeliveryModeUtils.addScheduleAnalogWorkflowToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class));

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void getFirstSentCourtesyMessage() {

        chooseDeliveryModeUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyInt());

        Mockito.verify(timelineService, Mockito.never()).getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    void getPlatformAddress() {

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(), Mockito.eq(0))).thenReturn(Mockito.any(NotificationRecipientInt.class));

        chooseDeliveryModeUtils.getPlatformAddress(Mockito.any(), Mockito.eq(0));

        Mockito.verify(addressBookService, Mockito.never()).getPlatformAddresses(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void getDigitalDomicile() {

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mockito.any(NotificationRecipientInt.class));

        //Mockito.
        chooseDeliveryModeUtils.getDigitalDomicile(Mockito.any(NotificationInt.class), Mockito.anyInt());
    }

    private List<NotificationRecipientInt> buildRecipients() {
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
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
                .sentAt(Instant.now())
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