package it.pagopa.pn.deliverypush.action2;


import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.action2.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartWorkflowHandler {
    private final LegalFactDao legalFactDao;
    private final NotificationService notificationService;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final ChooseDeliveryModeHandler chooseDeliveryType;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public StartWorkflowHandler(LegalFactDao legalFactDao, NotificationService notificationService,
                                CourtesyMessageUtils courtesyMessageUtils, ChooseDeliveryModeHandler chooseDeliveryType,
                                TimelineService timelineService, TimelineUtils timelineUtils) {
        this.legalFactDao = legalFactDao;
        this.notificationService = notificationService;
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.chooseDeliveryType = chooseDeliveryType;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process  - iun {}", iun);
        
        Notification notification = notificationService.getNotificationByIun(iun);
        legalFactDao.saveNotificationReceivedLegalFact(notification);

        for (NotificationRecipient recipient : notification.getRecipients()) {
            log.info("Notification recipient is {} for iun {}", recipient.getTaxId(), iun);
            // Per ogni recipient della notifica viene aggiunta l'accettazione della richiesta alla timeline ...
            addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, recipient.getTaxId()));
            //... inviato il messaggio di cortesia ...
            courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, recipient);
            //... e inizializzato il processo di scelta della tipologia di notificazione
            chooseDeliveryType.chooseDeliveryTypeAndStartWorkflow(notification, recipient);
        }
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
