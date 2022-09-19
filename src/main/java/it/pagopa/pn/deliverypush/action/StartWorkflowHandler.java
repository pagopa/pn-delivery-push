package it.pagopa.pn.deliverypush.action;


import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.utils.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StartWorkflowHandler {
    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final AttachmentUtils attachmentUtils;
    private final NotificationUtils notificationUtils;
    private final SchedulerService schedulerService;
    
    public StartWorkflowHandler(
            SaveLegalFactsService saveLegalFactsService,
            NotificationService notificationService,
            TimelineService timelineService,
            TimelineUtils timelineUtils,
            AttachmentUtils checkAttachmentUtils,
            NotificationUtils notificationUtils,
            SchedulerService schedulerService) {
        this.saveLegalFactsService = saveLegalFactsService;
        this.notificationService = notificationService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.attachmentUtils = checkAttachmentUtils;
        this.notificationUtils = notificationUtils;
        this.schedulerService = schedulerService;
    }
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     * @param firstDelivery
     */
    public void startWorkflow(String iun, boolean firstDelivery) {
        log.info("Start notification process - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        try {
            // aggiungo il controllo sullo stato attuale della notifica. Ho 3 possibili risultati: nuova, accettata, rifiutata
            TimelineUtils.CHECK_NEW_NOTIFICATION_RESULT notificationAlreadyAcceptedOrRefused = timelineUtils.checkNotificationAlreadyAcceptedOrRefused(iun);

            // la validazione e il salvataggio dell'accettazione lo eseguo SOLO se la notifica è nello stato NEW.
            // altrimenti vuol dire che son già passato di qui e quindi non lo devo rifare.
            if (notificationAlreadyAcceptedOrRefused == TimelineUtils.CHECK_NEW_NOTIFICATION_RESULT.NEW) {

                //Validazione degli allegati della notifica
                attachmentUtils.validateAttachment(notification);

                saveNotificationReceivedLegalFacts(notification);
            }

            // l'invio dei vari avvisi ai destinatari invece, viene fatto:
            // - se è una nuova notifica,
            // - se la notifica è già stata accettata ma firstDelivery è false (è il caso di un ritentativo per colpa di una eccezione, caso che non dovrebbe succedere ma che viene comunque gestito.)
            if ((notificationAlreadyAcceptedOrRefused == TimelineUtils.CHECK_NEW_NOTIFICATION_RESULT.NEW)
                || notificationAlreadyAcceptedOrRefused == TimelineUtils.CHECK_NEW_NOTIFICATION_RESULT.ALREADY_ACCEPTED && !firstDelivery)
            {
                if (notificationAlreadyAcceptedOrRefused == TimelineUtils.CHECK_NEW_NOTIFICATION_RESULT.ALREADY_ACCEPTED)
                {
                    log.warn("rescheduling recipient workflow for already accepted notification iun={}", iun);
                }

                for (NotificationRecipientInt recipient : notification.getRecipients()) {
                    Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
                    scheduleStartRecipientWorkflow(iun, recIndex);
                }
            }
            else
            {
                // qui ho 2 possibili casi:
                if (notificationAlreadyAcceptedOrRefused == TimelineUtils.CHECK_NEW_NOTIFICATION_RESULT.ALREADY_REFUSED)
                {
                    // - il caso di una notifica che era già stata rifiutata, ed è stata cmq riproposta, loggo
                    log.error("Skipping because already refused notification iun={}", iun);
                }
                else
                {
                    // - il caso di una notifica che era già stata accettata, ed è stata cmq riproposta come primo tentativo, loggo
                    log.error("Skipping because already accepted notification, and is still a firstDelivery iun={}", iun);
                }
            }
        } catch (PnValidationException ex) {
            handleValidationError(notification, ex);
        }
    }
    
    private void saveNotificationReceivedLegalFacts(NotificationInt notification) {
        // salvo il legalfactid di avvenuta ricezione da parte di PN
        String legalFactId = saveLegalFactsService.saveNotificationReceivedLegalFact(notification);

        // cambio lo stasto degli attachment in ATTACHED
        attachmentUtils.changeAttachmentsStatusToAttached(notification);

        // aggiungo l'evento in timeline
        addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId), notification);
    }
    
    private void scheduleStartRecipientWorkflow(String iun, Integer recIndex) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling start workflow for recipient schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.START_RECIPIENT_WORKFLOW);
    }
    
    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors =  ex.getProblem().getErrors().stream()
                .map(ProblemError::getDetail).collect(Collectors.toList());
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
