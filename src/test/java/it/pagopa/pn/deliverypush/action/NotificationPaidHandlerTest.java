package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.notificationpaid.NotificationPaidHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.PaymentInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.PaymentInformationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

class NotificationPaidHandlerTest {

    @Mock
    private TimelineService timelineService;

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationUtils notificationUtils;

    @Mock
    private PaymentInformationService paymentInformationService;

    private NotificationPaidHandler handler;

    @BeforeEach
    public void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationService = Mockito.mock(NotificationService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        paymentInformationService = Mockito.mock(PaymentInformationService.class);

        handler = new NotificationPaidHandler(timelineService, timelineUtils, notificationService, paymentInformationService);

    }

    @Test
    void handleNotificationPaid() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        Mockito.when(paymentInformationService.getIunFromPaTaxIdAndNoticeCode("001", "002")).thenReturn(buildNotificationCostResponseInt());
        Mockito.when(timelineService.getTimelineElement("001", buildElementId())).thenReturn(Optional.empty());
        Mockito.when(notificationService.getNotificationByIun("001")).thenReturn(NotificationInt.builder().iun("001").build());
        Mockito.when(timelineUtils.buildNotificationPaidTimelineElement(NotificationInt.builder().iun("001").build(), 2, instant)).thenReturn(buildTimelineElementInternal());

        handler.handleNotificationPaid("001", "002", instant);

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(buildTimelineElementInternal(), NotificationInt.builder().iun("001").build());
    }

    private PaymentInformation buildNotificationCostResponseInt() {
        return PaymentInformation.builder()
                .iun("001")
                .recipientIdx(2)
                .build();
    }

    private TimelineElementInternal buildTimelineElementInternal() {
        return TimelineElementInternal.builder()
                .iun("001")
                .elementId(buildElementId())
                .build();
    }

    private String buildElementId() {
        return TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun("001")
                        .build());
    }
}