package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.action.completionworkflow.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalProgressDetailsInt;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class CompletelyUnreachableUtilsTest {

    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;

    @Mock
    private TimelineService timelineService;

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private NotificationUtils notificationUtils;

    private CompletelyUnreachableUtils unreachableUtils;

    @BeforeEach
    void setUp() {
        paperNotificationFailedService = Mockito.mock(PaperNotificationFailedService.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        unreachableUtils = new CompletelyUnreachableUtils(paperNotificationFailedService, timelineService, timelineUtils, notificationUtils);
    }

    @Test
    void handleCompletelyUnreachableNotificationViewed() {
        NotificationInt notification = buildNotification();
        TimelineElementInternal t1 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa1").timestamp(Instant.now().minusMillis(30000))
                .details(SendDigitalProgressDetailsInt.builder().build())
                .build();
        NotificationRecipientInt recipient = buildRecipient("Galileo Bruno");
        PaperNotificationFailed notificationFailed = PaperNotificationFailed.builder()
                .iun(notification.getIun())
                .recipientId(recipient.getInternalId())
                .build();

        Mockito.when(timelineService.isPresentTimeLineElement(notification.getIun(), 1, TimelineEventId.NOTIFICATION_VIEWED)).thenReturn(Boolean.TRUE);
        Mockito.when(timelineUtils.buildCompletelyUnreachableTimelineElement(notification, 1)).thenReturn(t1);

        unreachableUtils.handleCompletelyUnreachable(notification, 1);
        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(t1, notification);
        Mockito.verify(paperNotificationFailedService, Mockito.times(0)).addPaperNotificationFailed(notificationFailed);
    }

    @Test
    void handleCompletelyUnreachableFalse() {
        NotificationInt notification = buildNotification();
        TimelineElementInternal t1 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa1").timestamp(Instant.now().minusMillis(30000))
                .details(SendDigitalProgressDetailsInt.builder().build())
                .build();

        NotificationRecipientInt recipient = buildRecipient("Galileo Bruno");
        PaperNotificationFailed notificationFailed = PaperNotificationFailed.builder()
                .iun(notification.getIun())
                .recipientId(recipient.getInternalId())
                .build();
        Mockito.when(timelineService.isPresentTimeLineElement(notification.getIun(), 1, TimelineEventId.NOTIFICATION_VIEWED)).thenReturn(Boolean.FALSE);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, 1)).thenReturn(recipient);
        Mockito.when(timelineUtils.buildCompletelyUnreachableTimelineElement(notification, 1)).thenReturn(t1);

        unreachableUtils.handleCompletelyUnreachable(notification, 1);

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(t1, notification);
        Mockito.verify(paperNotificationFailedService, Mockito.times(1)).addPaperNotificationFailed(notificationFailed);
    }

    private PhysicalAddressInt buildPhysicalAddressInt() {
        return new PhysicalAddressInt(
                "Palazzo dell'Inquisizione",
                "corso Italia 666",
                "Piano Terra (piatta)",
                "00100",
                "Roma",
                null,
                "RM",
                "IT"
        );
    }

    private NotificationRecipientInt buildRecipient(String denomination) {
        String defaultDenomination = StringUtils.hasText(denomination) ? denomination : "Galileo Bruno";
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination(defaultDenomination)
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(buildPhysicalAddressInt())
                .build();

        return rec1;
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
}