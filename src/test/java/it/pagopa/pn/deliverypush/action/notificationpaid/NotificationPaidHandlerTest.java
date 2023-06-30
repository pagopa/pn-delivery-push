package it.pagopa.pn.deliverypush.action.notificationpaid;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationpaid.NotificationPaidInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationPaidDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationPaidMapper;
import it.pagopa.pn.deliverypush.utils.UUIDCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

class NotificationPaidHandlerTest {

    private TimelineService timelineService;

    private TimelineUtils timelineUtils;

    private NotificationService notificationService;

    private NotificationPaidHandler handler;

    @BeforeEach
    public void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationService = Mockito.mock(NotificationService.class);

        handler = new NotificationPaidHandler(timelineService, timelineUtils, notificationService, Mockito.mock(UUIDCreatorUtils.class));

    }

    @Test
    void handleNotificationPaid() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        PnDeliveryPaymentEvent.Payload paymentEvent = PnDeliveryPaymentEvent.Payload.builder()
                .iun("001")
                .paymentDate(instant)
                .noticeCode("002")
                .recipientIdx(0)
                .creditorTaxId("taxId")
                .recipientType(PnDeliveryPaymentEvent.RecipientType.PF)
                .paymentType(PnDeliveryPaymentEvent.PaymentType.PAGOPA)
                .uncertainPaymentDate(true)
                .amount(1000)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("001")
                .sender(NotificationSenderInt.builder()
                        .paId("77777777777")
                        .build())
                .build();

        NotificationPaidInt notificationPaidInt = NotificationPaidMapper.messageToInternal(paymentEvent, null);
        String elementId = buildElementId(notificationPaidInt);
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(notificationPaidInt);

        Mockito.when(timelineService.getTimelineElement("001", buildElementId(notificationPaidInt))).thenReturn(Optional.empty());
        Mockito.when(notificationService.getNotificationByIun("001")).thenReturn(notification);
        Mockito.when(timelineUtils.buildNotificationPaidTimelineElement(notification, notificationPaidInt, elementId)).thenReturn(timelineElementInternal);

        handler.handleNotificationPaid(paymentEvent);

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }


    private TimelineElementInternal buildTimelineElementInternal(NotificationPaidInt notificationPaidInt) {
        return TimelineElementInternal.builder()
                .iun("001")
                .elementId(buildElementId(notificationPaidInt))
                .timestamp(Instant.now())
                .paId("77777777777")
                .category(TimelineElementCategoryInt.PAYMENT)
                .legalFactsIds(new ArrayList<>())
                .details(NotificationPaidDetailsInt.builder()
                        .recIndex(notificationPaidInt.getRecipientIdx())
                        .recipientType(notificationPaidInt.getRecipientType().getValue())
                        .amount(notificationPaidInt.getAmount())
                        .paymentSourceChannel(notificationPaidInt.getPaymentSourceChannel())
                        .creditorTaxId(notificationPaidInt.getCreditorTaxId())
                        .noticeCode(notificationPaidInt.getNoticeCode())
                        .build())
                .build();
    }

    private String buildElementId(NotificationPaidInt notificationPaidInt) {
        return handler.buildTimelineEventIdForPayment(notificationPaidInt);
    }

}