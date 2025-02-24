package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtilsImpl;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Base64Utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ChooseDeliveryModeUtilsTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private CourtesyMessageUtils courtesyMessageUtils;
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private NotificationUtils notificationUtils;

    @InjectMocks
    private ChooseDeliveryModeUtilsImpl chooseDeliveryModeUtils;

    private final Integer recIndex = 0;


    @Test
    void addAvailabilitySourceToTimeline() {

        Mockito.when(timelineUtils.buildAvailabilitySourceTimelineElement(0, null, null, false, 0)).thenReturn(Mockito.any(TimelineElementInternal.class));

        chooseDeliveryModeUtils.addAvailabilitySourceToTimeline(eq(0), Mockito.any(NotificationInt.class), Mockito.any(DigitalAddressSourceInt.class), eq(false));

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void addScheduleAnalogWorkflowToTimeline() {

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("el1")
                .timestamp((Instant.parse("2021-09-16T15:24:00.00Z")))
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        NotificationInt notificationInt = buildNotification();

        Mockito.when(timelineUtils.buildScheduleAnalogWorkflowTimeline(notificationInt, recIndex, Instant.EPOCH.plusMillis(10))).thenReturn(timelineElementInternal);

        chooseDeliveryModeUtils.addScheduleAnalogWorkflowToTimeline(recIndex, notificationInt, Instant.EPOCH.plusMillis(10));

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notificationInt);
    }

    @Test
    void getFirstSentCourtesyMessage() {

        chooseDeliveryModeUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyInt());

        Mockito.verify(timelineService, Mockito.never()).getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    void getPlatformAddress() {

        NotificationInt notificationInt = buildNotification();
        NotificationRecipientInt notificationRecipientInt = buildNotificationRecipientInt();

        Mockito.when(notificationUtils.getRecipientFromIndex(notificationInt, recIndex)).thenReturn(notificationRecipientInt);
        Mockito.when(addressBookService.getPlatformAddresses(notificationRecipientInt.getInternalId(), notificationInt.getSender().getPaId())).thenReturn(Optional.of(notificationRecipientInt.getDigitalDomicile()));

        Optional<LegalDigitalAddressInt> tmp = chooseDeliveryModeUtils.getPlatformAddress(notificationInt, recIndex);

        Assertions.assertNotNull(tmp);
    }

    @Test
    void getDigitalDomicile() {

        NotificationInt notificationInt = buildNotification();
        NotificationRecipientInt notificationRecipientInt = buildNotificationRecipientInt();

        Mockito.when(notificationUtils.getRecipientFromIndex(notificationInt, recIndex)).thenReturn(notificationRecipientInt);

        LegalDigitalAddressInt tmp = chooseDeliveryModeUtils.getDigitalDomicile(notificationInt, recIndex);

        Assertions.assertNotNull(tmp);
    }

    private NotificationRecipientInt buildNotificationRecipientInt() {
        return NotificationRecipientInt.builder()
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