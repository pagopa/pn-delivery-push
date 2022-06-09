package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationViewedHandler {

    private final LegalFactDao legalFactStore;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final InstantNowSupplier instantNowSupplier;
    private final NotificationUtils notificationUtils;
    
    public NotificationViewedHandler(TimelineService timelineService, LegalFactDao legalFactStore,
                                     PaperNotificationFailedService paperNotificationFailedService, NotificationService notificationService,
                                     TimelineUtils timelineUtils, InstantNowSupplier instantNowSupplier, NotificationUtils notificationUtils) {
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
        this.notificationUtils = notificationUtils;
    }
    
    public void handleViewNotification(String iun, Integer recIndex) {
        log.debug("Start HandleViewNotification - iun={}", iun);



        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(iun, recIndex);
        
        //I processi collegati alla visualizzazione di una notifica vengono effettuati solo la prima volta che la stessa viene visualizzata
        if(!isNotificationAlreadyViewed){
            log.info("Start view notification process");
            
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            log.debug("handleViewNotification get recipient ok - iun={} id={}", iun, recIndex);

            NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
            String legalFactId = legalFactStore.saveNotificationViewedLegalFact(notification, recipient, instantNowSupplier.get());

            addTimelineElement(timelineUtils.buildNotificationViewedTimelineElement(iun, recIndex, legalFactId));

            paperNotificationFailedService.deleteNotificationFailed(recipient.getTaxId(), iun); //Viene eliminata l'eventuale istanza di notifica fallita dal momento che la stessa è stata letta
        } else {
            log.debug("Notification is already viewed");
        }

        log.debug("End HandleViewNotification - iun={} id={}", iun, recIndex);
    }

    private void addTimelineElement(TimelineElementInternal element) {
        timelineService.addTimelineElement(element);
    }
}
