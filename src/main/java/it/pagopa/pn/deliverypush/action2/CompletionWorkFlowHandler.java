package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class CompletionWorkFlowHandler {
    public static final int SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT = 7;
    public static final int SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT = 15;
    public static final int SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT = 10;
    public static final int SCHEDULING_DAYS_FAILURE_ANALOG_REFINEMENT = 10;

    private final NotificationService notificationService;
    private final SchedulerService scheduler;
    private final ExternalChannelUtils externalChannelUtils;
    private final TimelineService timelineService;
    private final CompletelyUnreachableUtils completelyUnreachableService;
    private final TimelineUtils timelineUtils;

    public CompletionWorkFlowHandler(NotificationService notificationService,
                                     SchedulerService scheduler, ExternalChannelUtils externalChannelUtils,
                                     TimelineService timelineService, CompletelyUnreachableUtils completelyUnreachableUtils,
                                     TimelineUtils timelineUtils) {
        this.notificationService = notificationService;
        this.scheduler = scheduler;
        this.externalChannelUtils = externalChannelUtils;
        this.timelineService = timelineService;
        this.completelyUnreachableService = completelyUnreachableUtils;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle necessary steps to complete the digital workflow
     *
     * @param taxId            User identifier
     * @param iun              Notification unique identifier
     * @param notificationDate Conclusion workflow date
     * @param status           Conclusion workflow status
     */
    public void completionDigitalWorkflow(String taxId, String iun, Instant notificationDate, DigitalAddress address, EndWorkflowStatus status) {
        log.info("Digital workflow completed with status {} IUN {} id {}", status, iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        //TODO Capire meglio quali sono le operazioni da realizzare nel perfezionamento e quali in questa fase

        //TODO Capire meglio quali sono le informazioni necessarie e realizzarlo
        //legalFactStore.savePecDeliveryWorkflowLegalFact( receivePecActions, notification, addresses );

        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement(timelineUtils.buildSuccessDigitalWorkflowTimelineElement(taxId, iun, address));
                    scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT);
                    break;
                case FAILURE:
                    //TODO Generare avviso mancato recapito
                    sendSimpleRegisteredLetter(notification, recipient);
                    addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(taxId, iun));
                    scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT);
                    break;
                default:
                    handleError(taxId, iun, status);
            }
        } else {
            handleError(taxId, iun, null);
        }
    }

    /**
     * Sent notification by simple registered letter
     */
    private void sendSimpleRegisteredLetter(Notification notification, NotificationRecipient recipient) {
        //Al termine del workflow digitale se non si è riusciti ad contattare in nessun modo il recipient, viene inviata una raccomanda semplice

        PhysicalAddress physicalAddress = recipient.getPhysicalAddress();

        if (physicalAddress != null) {
            log.info("Sending simple registered letter for iun {} id {}", notification.getIun(), recipient.getTaxId());
            externalChannelUtils.sendNotificationForRegisteredLetter(notification, physicalAddress, recipient);
        } else {
            log.info("Simple registered letter can't be send, there isn't physical address for recipient. iun {} id {}", notification.getIun(), recipient.getTaxId());
        }
    }

    /**
     * Handle necessary steps to complete analog workflow.
     *
     * @param taxId            User identifier
     * @param iun              Notification unique identifier
     * @param notificationDate Conclusion workflow date
     * @param status           Conclusion workflow status
     */
    public void completionAnalogWorkflow(String taxId, String iun, Instant notificationDate, PhysicalAddress usedAddress, EndWorkflowStatus status) {
        log.info("Analog workflow completed with status {} IUN {} id {}", status, iun, taxId);

        switch (status) {
            case SUCCESS:
                addTimelineElement(timelineUtils.buildSuccessAnalogWorkflowTimelineElement(taxId, iun, usedAddress));
                scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT);
                break;
            case FAILURE:
                addTimelineElement(timelineUtils.buildFailureAnalogWorkflowTimelineElement(taxId, iun));
                completelyUnreachableService.handleCompletelyUnreachable(iun, taxId);
                scheduleRefinement(iun, taxId, notificationDate, SCHEDULING_DAYS_FAILURE_ANALOG_REFINEMENT);
                break;
            default:
                handleError(taxId, iun, status);
        }
    }

    private void scheduleRefinement(String iun, String taxId, Instant notificationDate, int schedulingDays) {
        Instant schedulingDate = notificationDate.plus(schedulingDays, ChronoUnit.DAYS);
        log.info("Schedule refinement in {}", schedulingDate);
        scheduler.scheduleEvent(iun, taxId, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }


    private void handleError(String taxId, String iun, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, taxId);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + taxId);
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
