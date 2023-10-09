package it.pagopa.pn.deliverypush.action.notificationpaid;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationpaid.NotificationPaidInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationPaidMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class NotificationPaidHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;

    public NotificationPaidHandler(TimelineService timelineService,
                                   TimelineUtils timelineUtils,
                                   NotificationService notificationService) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationService = notificationService;
    }

    public void handleNotificationPaid(PnDeliveryPaymentEvent.Payload paymentEventPayload) {

        NotificationPaidInt notificationPaidInt = NotificationPaidMapper.messageToInternal(paymentEventPayload);

        // attualmente controllo inutile per i pagamenti f24, in quanto inserendo un UUID, non verrà mai trovata la timeline,
        // ma quando (se) verrà popolato un vero id, il controllo sarà necessario anche per gli f24
        String elementId = buildTimelineEventIdForPayment(notificationPaidInt);
        Optional<TimelineElementInternal> timelineElementOpt = getNotificationPaidTimelineElement(notificationPaidInt.getIun(), elementId);

        if (timelineElementOpt.isEmpty()) {
            //Se il pagamento non è già avvenuto per questo (IUN,creditoTaxId,noticeCode)
            handleInsertNotificationPaidTimelineElement(notificationPaidInt, elementId);
        } else {
            //Pagamento già avvenuto
            log.info("Event PAYMENT already received: {}", paymentEventPayload);
        }
    }

    private void handleInsertNotificationPaidTimelineElement(NotificationPaidInt notificationPaidInt, String elementId) {
        log.info("New event PAYMENT, start process to insert: {} ",notificationPaidInt);

        NotificationInt notification = notificationService.getNotificationByIun(notificationPaidInt.getIun());

        TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationPaidTimelineElement(notification, notificationPaidInt, elementId);
        timelineService.addTimelineElement(timelineElementInternal, notification);
        log.info("Payment process complete: {}", notificationPaidInt);
    }

    protected String buildTimelineEventIdForPayment(NotificationPaidInt notificationPaidInt) {
        return TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun(notificationPaidInt.getIun())
                        .creditorTaxId(notificationPaidInt.getCreditorTaxId())
                        .noticeCode(notificationPaidInt.getNoticeCode())
                        .build());
    }

    private Optional<TimelineElementInternal> getNotificationPaidTimelineElement(String iun, String elementId) {

        return timelineService.getTimelineElement(iun, elementId);

    }

}
