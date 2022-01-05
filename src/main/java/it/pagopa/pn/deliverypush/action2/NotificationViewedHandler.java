package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationViewedHandler {

    private final LegalFactUtils legalFactStore;
    private final PaperNotificationFailedDao paperNotificationFailedDao;
    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;

    public NotificationViewedHandler(TimelineService timelineService, LegalFactUtils legalFactStore,
                                     PaperNotificationFailedDao paperNotificationFailedDao, NotificationService notificationService,
                                     TimelineUtils timelineUtils) {
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedDao = paperNotificationFailedDao;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
    }

    @StreamListener(condition = "NOTIFICATION_VIEWED")
    public void handleViewNotification(String iun, String taxId) {
        Notification notification = notificationService.getNotificationByIun(iun);

        addTimelineElement(timelineUtils.buildNotificationViewedTimelineElement(iun, taxId));
        //TODO Da aggiungere quando verranno modificati i vari legalFacts
        //legalFactStore.saveNotificationViewedLegalFact(action, notification);
        paperNotificationFailedDao.deleteNotificationFailed(taxId, iun); //Viene eliminata l'istanza di notifica fallita dal momento che la stessa è stata letta
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
