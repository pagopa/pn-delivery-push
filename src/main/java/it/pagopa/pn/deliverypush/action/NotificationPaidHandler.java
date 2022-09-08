package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
public class NotificationPaidHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final NotificationCostService notificationCostService;
    
    public NotificationPaidHandler(TimelineService timelineService,
                                   TimelineUtils timelineUtils,
                                   NotificationService notificationService,
                                   NotificationUtils notificationUtils,
                                   NotificationCostService notificationCostService) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.notificationCostService = notificationCostService;
    }

    public void handleNotificationPaid(String paTaxId, String noticeNumber, Instant paymentDate){
        log.debug("Start handle notification paid - paTaxId={} noticeNumber={} paymentDate={}", LogUtils.maskTaxId(paTaxId), noticeNumber, paymentDate);
        
        NotificationCostResponseInt notificationCostResponseInt = getIunFromPaTaxIdAndNoticeNumber(paTaxId, noticeNumber);
        String iun = notificationCostResponseInt.getIun();
        log.debug("Get iun complete in handleNotificationPaid - iun={} paTaxId={} ", iun, LogUtils.maskTaxId(paTaxId));

        Optional<TimelineElementInternal> timelineElementOpt = getNotificationPaidTimelineElement(iun);

        if( timelineElementOpt.isEmpty() ){
            //Se il pagamento non è già avvenuto per questo IUN
            handleInsertNotificationPaidTimelineElement(paTaxId, paymentDate, iun, notificationCostResponseInt.getRecipientIdx());
        }else {
            //Pagamento già avvenuto
            log.info("Notification has already been paid - iun={} paTaxId={} ", iun, paTaxId);
        }
    }

    private void handleInsertNotificationPaidTimelineElement(String paTaxId, Instant paymentDate, String iun, Integer recIndex) {
        log.info("Notification has not already been paid, start process to insert payment - iun={} paTaxId={} ", iun, paTaxId);
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        timelineService.addTimelineElement( timelineUtils.buildNotificationPaidTimelineElement(notification, recIndex, paymentDate), notification );
        log.info("Payment process complete - iun={} paTaxId={} ", iun, paTaxId);
    }

    private Optional<TimelineElementInternal> getNotificationPaidTimelineElement(String iun) {
        String elementId = TimelineEventId.NOTIFICATION_PAID.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    private NotificationCostResponseInt getIunFromPaTaxIdAndNoticeNumber(String paTaxId, String noticeNumber) {
        return notificationCostService.getIunFromPaTaxIdAndNoticeCode(paTaxId, noticeNumber);
    }

}