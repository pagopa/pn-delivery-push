package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.FailureWorkflow;
import it.pagopa.pn.api.dto.notification.timeline.SuccessWorkflow;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
public class CompletionWorkFlowHandler {
    public static final int SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT = 7;
    public static final int SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT = 15;
    public static final int SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT = 10;
    public static final int SCHEDULING_DAYS_FAILURE_ANALOG_REFINEMENT = 10;
    private LegalFactGeneratorService legalFactGenerator;
    private NotificationService notificationService;
    private SchedulerService scheduler;
    private ExternalChannelService externalChannelService;
    private TimelineService timelineService;

    /**
     * Handle necessary steps to complete the digital workflow
     *
     * @param taxId            User identifier
     * @param iun              Notification unique identifier
     * @param notificationDate Conclusion workflow date
     * @param status           Conclusion workflow status
     */
    public void completionDigitalWorkflow(String taxId, String iun, Instant notificationDate, EndWorkflowStatus status) {
        log.info("Workflow completed with status {} IUN {} id {}", status, iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        legalFactGenerator.workflowStep(notification);

        switch (status) {
            case SUCCESS:
                addSuccessWorkflowToTimeline(taxId, iun);
                scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT);
                break;
            case FAILURE:
                //TODO Generare avviso mancato recapito
                legalFactGenerator.nonDeliveryMessage(notification);
                sendSimpleRegisteredLetter(notification, recipient);
                addFailureWorkflowToTimeline(taxId, iun);
                scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT);
                break;
            default:
                log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, taxId);
                throw new PnInternalException("Specified contactPhase " + status + " does not exist. Iun " + iun + " id" + taxId);
        }

    }

    private void scheduleRefinement(String iun, String taxId, Instant notificationDate, int schedulingDays) {
        //TODO Schedulare con iun e taxId
        Instant schedulingDate = notificationDate.plus(schedulingDays, ChronoUnit.DAYS);
        scheduler.schedulEvent(iun, taxId, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }

    /**
     * Sent notification by simple registered letter
     */
    private void sendSimpleRegisteredLetter(Notification notification, NotificationRecipient recipient) {
        //Al termine del workflow digitale se non si è riusciti ad contattare in nessun modo il recipient, viene inviata una raccomanda semplice

        PhysicalAddress physicalAddress = recipient.getPhysicalAddress();

        if (physicalAddress != null) {
            log.info("Sending simple registered letter for iun {} id {}", notification.getIun(), recipient.getTaxId());
            externalChannelService.sendNotificationForRegisteredLetter(notification, physicalAddress, recipient);
        } else {
            log.info("Simple registered letter can't be send, there isn't physical address for recipient. iun {} id {}", notification.getIun(), recipient.getTaxId());
        }
    }

    private void addSuccessWorkflowToTimeline(String taxId, String iun) {
        log.debug("AddSuccessWorkflowToTimeline for iun {} id {}", iun, taxId);

        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SUCCESS_WORKFLOW)
                .iun(iun)
                .details(SuccessWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build());
    }

    private void addFailureWorkflowToTimeline(String taxId, String iun) {
        log.debug("addFailureWorkflowToTimeline for iun {} id {}", iun, taxId);

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

        Notification notification = notificationService.getNotificationByIun(iun);

        legalFactGenerator.workflowStep(notification);
        legalFactGenerator.receivedMessage(notification);//avviso avvenuta ricezione
        switch (status) {
            case SUCCESS:
                addSuccessWorkflowToTimeline(taxId, iun);
                scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT);
                break;
            case FAILURE:
                addFailureWorkflowToTimeline(taxId, iun);
                scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_FAILURE_ANALOG_REFINEMENT);
                break;
            default:
                //TODO Gestire errore
                break;
        }

    }

}
