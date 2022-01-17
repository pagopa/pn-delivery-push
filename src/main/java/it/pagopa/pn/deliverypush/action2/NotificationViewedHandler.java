package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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
    
    //TODO Capire se si può eliminare e tenere la Action già presente
    //@StreamListener(condition = "NOTIFICATION_VIEWED")
    public void handleViewNotification(String iun, String taxId) {
        log.info("Start HandleViewNotification - iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);

        addTimelineElement(timelineUtils.buildNotificationViewedTimelineElement(iun, taxId));
        //TODO Da aggiungere se non eliminato modificando la logica del metodo
        //legalFactStore.saveNotificationViewedLegalFact(action, notification);
        paperNotificationFailedDao.deleteNotificationFailed(taxId, iun); //Viene eliminata l'istanza di notifica fallita dal momento che la stessa è stata letta

        log.debug("End HandleViewNotification - iun {} id {}", iun, taxId);
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
