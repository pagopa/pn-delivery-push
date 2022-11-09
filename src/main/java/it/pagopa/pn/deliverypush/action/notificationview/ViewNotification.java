package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class ViewNotification {
    private final InstantNowSupplier instantNowSupplier;
    private final SaveLegalFactsService legalFactStore;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final NotificationCost notificationCost;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;

    public ViewNotification(InstantNowSupplier instantNowSupplier,
                            SaveLegalFactsService legalFactStore,
                            PaperNotificationFailedService paperNotificationFailedService,
                            NotificationCost notificationCost,
                            TimelineUtils timelineUtils, 
                            TimelineService timelineService) {
        this.instantNowSupplier = instantNowSupplier;
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.notificationCost = notificationCost;
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
    }

    public void startVewNotificationProcess(NotificationInt notification,
                                     NotificationRecipientInt recipient,
                                     Integer recIndex,
                                     String raddType,
                                     String raddTransactionId, 
                                     Instant eventTimestamp
    ) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), recIndex);

        String legalFactId = legalFactStore.saveNotificationViewedLegalFact(notification, recipient, instantNowSupplier.get());

        Integer cost = notificationCost.getNotificationCost(notification, recIndex);
        log.debug("Notification cost is {} - iun {} id {}",cost, notification.getIun(), recIndex);

        addTimelineElement(
                timelineUtils.buildNotificationViewedTimelineElement(notification, recIndex, legalFactId, cost, raddType, raddTransactionId, eventTimestamp),
                notification
        ) ;

        paperNotificationFailedService.deleteNotificationFailed(recipient.getInternalId(), notification.getIun()); //Viene eliminata l'eventuale istanza di notifica fallita dal momento che la stessa è stata letta
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
