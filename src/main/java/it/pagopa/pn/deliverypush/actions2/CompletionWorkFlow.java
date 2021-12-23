package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.timeline.FailureWorkflow;
import it.pagopa.pn.api.dto.notification.timeline.SuccessWorkflow;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class CompletionWorkFlow {
    public static final int SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT = 7;
    public static final int SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT = 15;
    public static final int SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT = 10;
    public static final int SCHEDULING_DAYS_FAILURE_ANALOG_REFINEMENT = 10;
    private LegalFactGenerator legalFactGenerator;
    private NotificationDao notificationDao;
    private Scheduler scheduler;
    private ExternalChannel externalChannel;
    private TimelineService timelineService;

    /**
     * Handle necessary steps to complete the digital.
     *
     * @param taxId            User identifier
     * @param iun              Notification unique identifier
     * @param notificationDate Conclusion workflow date
     * @param status           Conclusion workflow status
     */
    public void completionDigitalWorkflow(String taxId, String iun, Instant notificationDate, EndWorkflowStatus status) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {

            Notification notification = optNotification.get();
            legalFactGenerator.conclusionStep(notification);
            switch (status) {
                case SUCCESS:
                    addSuccessWorkflowToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT);
                    break;
                case FAILURE:
                    //TODO Generare avviso mancato recapito
                    legalFactGenerator.nonDeliveryMessage(notification);
                    sendRegisteredLetter(notification);
                    addFailureWorkflowToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT);
                    break;
                default:
                    //TODO Gestire casistica di errore
                    break;
            }
        }
    }

    private void scheduleRefinement(Instant notificationDate, int schedulingDays) {
        Instant schedulingDate = notificationDate.plus(schedulingDays, ChronoUnit.DAYS);
        scheduler.schedulEvent(schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }

    /**
     * Sent notification by simple registered letter
     */
    private void sendRegisteredLetter(Notification notification) {
        String address = null; //TODO Ottiene indirizzo per invio raccomandata semplice
        externalChannel.sendNotificationForRegisteredLetter(notification, address);
    }

    private void addSuccessWorkflowToTimeline(String taxId, String iun) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SUCCESS_WORKFLOW)
                .iun(iun)
                .details(SuccessWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build());
    }

    private void addFailureWorkflowToTimeline(String taxId, String iun) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.FAILURE_WORKFLOW)
                .iun(iun)
                .details(FailureWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build());
    }

    /**
     * Handle necessary steps to complete analog workflow.
     *
     * @param taxId            User identifier
     * @param iun              Notification unique identifier
     * @param notificationDate Conclusion workflow date
     * @param status           Conclusion workflow status
     */
    public void completionAnalogWorkflow(String taxId, String iun, Instant notificationDate, EndWorkflowStatus status) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            legalFactGenerator.conclusionStep(notification);
            legalFactGenerator.receivedMessage(notification);//avviso avvenuta ricezione
            switch (status) {
                case SUCCESS:
                    addSuccessWorkflowToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT);
                    break;
                case FAILURE:
                    //Aggiunge alla tabelle irreperibili totali
                    addFailureWorkflowToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, SCHEDULING_DAYS_FAILURE_ANALOG_REFINEMENT);
                    break;
                default:
                    //Gestire errore
                    break;
            }
        }
    }

}
