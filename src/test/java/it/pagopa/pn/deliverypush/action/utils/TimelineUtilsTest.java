package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancellationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.mock;

class TimelineUtilsTest {

    @Mock
    private TimelineService timelineService;

    private TimelineUtils timelineUtils;

    @BeforeEach
    void setUp() {
        timelineService = mock(TimelineService.class);
        timelineUtils = new TimelineUtils(timelineService);
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
    void checkIsNotificationRefined() {
        String iun = "IUN-checkIsNotificationRefined";

        Mockito.when(timelineService.getTimelineElement(Mockito.eq(iun), Mockito.anyString())).thenReturn(Optional.of(TimelineElementInternal.builder().build()));

        boolean isNotificationRefined = timelineUtils.checkIsNotificationRefined(iun, 0);
        Assertions.assertTrue(isNotificationRefined);
    }

    @Test
    void checkIsNotificationRefinedFalse() {
        String iun = "IUN-checkIsNotificationRefinedFalse";

        Mockito.when(timelineService.getTimelineElement(Mockito.eq(iun), Mockito.anyString())).thenReturn(Optional.empty());

        boolean isNotificationRefined = timelineUtils.checkIsNotificationRefined(iun, 0);
        Assertions.assertFalse(isNotificationRefined);
    }

    @Test
    void checkIsNotificationCancellationNotRequested() {
        String iun = "IUN-checkIsNotificationCancellationNotRequested";

        Mockito.when(timelineService.getTimelineElement(Mockito.eq(iun), Mockito.anyString())).thenReturn(Optional.empty());

        boolean isNotificationCancellationRequested = timelineUtils.checkIsNotificationCancellationRequested(iun);
        Assertions.assertFalse(isNotificationCancellationRequested);
    }

    @Test
    void checkIsNotificationCancellationRequested() {
        String iun = "IUN-checkIsNotificationCancellationRequested";

        String timelineEventId = TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST)
                .elementId(timelineEventId)
                .details(NotificationCancellationRequestDetailsInt.builder()
                        .build())
                .build();

        Mockito.when(timelineService.getTimelineElement(iun, timelineEventId)).thenReturn(Optional.of(timelineElementInternal));

        boolean isNotificationCancellationRequested = timelineUtils.checkIsNotificationCancellationRequested(iun);
        Assertions.assertTrue(isNotificationCancellationRequested);
    }

    @Test
    void checkIsNotificationCancelledLegalFactId_Found() {
        String iun = "testIun";
        String legalFactId = "legalFactId";

        String elementId = TimelineEventId.NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder().key(PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX + legalFactId).build()))
                .build();

        Mockito.when(timelineService.getTimelineElement(iun, elementId)).thenReturn(Optional.of(timelineElementInternal));

        boolean isCancelled = timelineUtils.checkIsNotificationCancelledLegalFactId(iun, legalFactId);

        Assertions.assertTrue(isCancelled);
    }

    @Test
    void checkIsNotificationCancelledLegalFactId_NotFound() {
        String iun = "testIun";
        String legalFactId = "legalFactId";

        String elementId = TimelineEventId.NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .legalFactsIds(Collections.singletonList(LegalFactsIdInt.builder().key(PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX + "differentLegalFactId").build()))
                .build();

        Mockito.when(timelineService.getTimelineElement(iun, elementId)).thenReturn(Optional.of(timelineElementInternal));

        boolean isCancelled = timelineUtils.checkIsNotificationCancelledLegalFactId(iun, legalFactId);

        Assertions.assertFalse(isCancelled);
    }

    @Test
    void buildNotificationRaddRetrieveTimelineElement() {
        NotificationInt notification = buildNotification();
        Integer recIndex = 1;
        Instant eventTimestamp = Instant.now();

        RaddInfo raddInfo = RaddInfo.builder()
                .build();


        String timelineEventIdExpected = "NOTIFICATION_RADD_RETRIEVED.IUN_Example_IUN_1234_Test.RECINDEX_1";

        TimelineElementInternal actual = timelineUtils.buildNotificationRaddRetrieveTimelineElement(
                notification,
                recIndex,
                raddInfo,
                eventTimestamp
        );

        Assertions.assertAll(
                () -> Assertions.assertEquals("Example_IUN_1234_Test", actual.getIun()),
                () -> Assertions.assertEquals(timelineEventIdExpected,actual.getElementId())
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
                .documents(Collections.singletonList(
                                NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64.getEncoder().encodeToString("sha256_doc01".getBytes())))
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

}