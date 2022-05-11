package it.pagopa.pn.deliverypush.action2;


import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action2.utils.CheckAttachmentUtils;
import it.pagopa.pn.deliverypush.action2.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StartWorkflowHandler {
    private final LegalFactUtils legalFactUtils;
    private final NotificationService notificationService;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final ChooseDeliveryModeHandler chooseDeliveryType;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final CheckAttachmentUtils checkAttachmentUtils;
    private final NotificationUtils notificationUtils;
    
    public StartWorkflowHandler(LegalFactUtils legalFactUtils, NotificationService notificationService,
                                CourtesyMessageUtils courtesyMessageUtils, ChooseDeliveryModeHandler chooseDeliveryType,
                                TimelineService timelineService, TimelineUtils timelineUtils, CheckAttachmentUtils checkAttachmentUtils, 
                                NotificationUtils notificationUtils) {
        this.legalFactUtils = legalFactUtils;
        this.notificationService = notificationService;
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.chooseDeliveryType = chooseDeliveryType;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.checkAttachmentUtils = checkAttachmentUtils;
        this.notificationUtils = notificationUtils;
    }
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process - iun {}", iun);
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        try{
            //Validazione degli allegati della notifica
            checkAttachmentUtils.validateAttachment(notification);

            String legalFactId = legalFactUtils.saveNotificationReceivedLegalFact(notification);

            addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId));
            
            //Start del workflow per ogni recipient della notifica
            for (NotificationRecipientInt recipient : notification.getRecipients()) {
                Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
                startNotificationWorkflowForRecipient(notification, recIndex);
            }
        }catch (PnValidationException ex){
            handleValidationError(notification, ex);
        }
    }

    private void startNotificationWorkflowForRecipient(NotificationInt notification, Integer recIndex) {
        log.info("Start notification workflow - iun {} id {}", notification.getIun(), recIndex);
        //... Invio messaggio di cortxesia ...
        courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, recIndex);
        //... e inizializzato il processo di scelta della tipologia di notificazione
        chooseDeliveryType.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);
    }

    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors =  ex.getValidationErrors().stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement(timelineUtils.buildRefusedRequestTimelineElement(notification, errors));
    }

    private void addTimelineElement(TimelineElementInternal element) {
        timelineService.addTimelineElement(element);
    }

}
