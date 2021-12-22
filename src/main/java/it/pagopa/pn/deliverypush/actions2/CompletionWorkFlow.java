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
    private LegalFactGenerator legalFactGenerator;
    private NotificationDao notificationDao;
    private Scheduler scheduler;
    private ExternalChannel externalChannel;
    private TimelineService timelineService;

    public void endOfDigitalWorkflow(String taxId, String iun, Instant notificationDate, EndWorkflowStatus status) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            legalFactGenerator.conclusionStep(notification);
            switch (status) {
                case SUCCESS:
                    addSuccessToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, 7);
                    break;
                case FAILURE:
                    //TODO Generare avviso mancato recapito
                    legalFactGenerator.nonDeliveryMessage(notification);
                    sendRegisteredLetter(notification);
                    addFailureToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, 15);
                    break;
            }
        }
    }

    private void scheduleRefinement(Instant notificationDate, int i) {
        Instant schedulingDate = notificationDate.plus(i, ChronoUnit.DAYS);
        scheduler.schedulEvent(schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }

    private void sendRegisteredLetter(Notification notification) {
        String address = null; //TODO Ottiene indirizzo per invio raccomandata semplice
        externalChannel.sendNotificationForRegisteredLetter(notification, address);
    }


    private void addSuccessToTimeline(String taxId, String iun) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SUCCESS_WORKFLOW)
                .iun(iun)
                .details(SuccessWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build());
    }

    private void addFailureToTimeline(String taxId, String iun) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.FAILURE_WORKFLOW)
                .iun(iun)
                .details(FailureWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build());
    }

    public void endOfAnalogWorkflow(String taxId, String iun, Instant notificationDate, EndWorkflowStatus status) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            legalFactGenerator.conclusionStep(notification);
            legalFactGenerator.receivedMessage(notification);//avviso avvenuta ricezione
            switch (status) {
                case SUCCESS:
                    addSuccessToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, 10);
                    break;
                case FAILURE:
                    //Aggiunge alla tabelle irreperibili totali
                    addFailureToTimeline(taxId, iun);
                    scheduleRefinement(notificationDate, 10);
                    break;
            }
        }
    }

}
