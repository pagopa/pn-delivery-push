package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedback;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CompletionWorkFlowHandler {
    private final NotificationService notificationService;
    private final SchedulerService scheduler;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final TimelineService timelineService;
    private final CompletelyUnreachableUtils completelyUnreachableService;
    private final TimelineUtils timelineUtils;
    private final LegalFactUtils legalFactUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public CompletionWorkFlowHandler(NotificationService notificationService, SchedulerService scheduler,
                                     ExternalChannelSendHandler externalChannelSendHandler, TimelineService timelineService,
                                     CompletelyUnreachableUtils completelyUnreachableUtils, TimelineUtils timelineUtils,
                                     LegalFactUtils legalFactUtils, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.notificationService = notificationService;
        this.scheduler = scheduler;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.timelineService = timelineService;
        this.completelyUnreachableService = completelyUnreachableUtils;
        this.timelineUtils = timelineUtils;
        this.legalFactUtils = legalFactUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
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

        generatePecDeliveryWorkflowLegalFact(taxId, iun, notification, recipient);

        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement(timelineUtils.buildSuccessDigitalWorkflowTimelineElement(taxId, iun, address));
                    scheduleRefinement(iun, taxId, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessDigitalRefinement());
                    break;
                case FAILURE:
                    //TODO Generare avviso mancato recapito
                    sendSimpleRegisteredLetter(notification, recipient);
                    addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(taxId, iun));
                    scheduleRefinement(iun, taxId, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureDigitalRefinement());
                    break;
                default:
                    handleError(taxId, iun, status);
            }
        } else {
            handleError(taxId, iun, null);
        }
    }

    private void generatePecDeliveryWorkflowLegalFact(String taxId, String iun, Notification notification, NotificationRecipient recipient) {
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);
        List<SendDigitalFeedback> listFeedbackFromExtChannel = timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxId(timelineElement, taxId))
                .map(timelineElement -> (SendDigitalFeedback) timelineElement.getDetails())
                .collect(Collectors.toList());

        legalFactUtils.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient);
    }

    private boolean filterTimelineForTaxId(TimelineElement el, String taxId) {
        boolean availableCategory = TimelineElementCategory.SEND_DIGITAL_FEEDBACK.equals(el.getCategory());
        if (availableCategory) {
            SendDigitalFeedback details = (SendDigitalFeedback) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    /**
     * Sent notification by simple registered letter
     */
    private void sendSimpleRegisteredLetter(Notification notification, NotificationRecipient recipient) {
        //Al termine del workflow digitale se non si è riusciti ad contattare in nessun modo il recipient, viene inviata una raccomanda semplice

        PhysicalAddress physicalAddress = recipient.getPhysicalAddress();

        if (physicalAddress != null) {
            log.info("Sending simple registered letter  - iun {} id {}", notification.getIun(), recipient.getTaxId());
            externalChannelSendHandler.sendNotificationForRegisteredLetter(notification, physicalAddress, recipient);
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

        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement(timelineUtils.buildSuccessAnalogWorkflowTimelineElement(taxId, iun, usedAddress));
                    scheduleRefinement(iun, taxId, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessAnalogRefinement());
                    break;
                case FAILURE:
                    addTimelineElement(timelineUtils.buildFailureAnalogWorkflowTimelineElement(taxId, iun));
                    completelyUnreachableService.handleCompletelyUnreachable(iun, taxId);
                    scheduleRefinement(iun, taxId, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureAnalogRefinement());
                    break;
                default:
                    handleError(taxId, iun, status);
            }
        } else {
            handleError(taxId, iun, status);
        }
    }

    private void scheduleRefinement(String iun, String taxId, Instant notificationDate, Duration scheduleTime) {
        Instant schedulingDate = notificationDate.plus(scheduleTime);
        log.info("Schedule refinement in {}", schedulingDate);
        timelineService.addTimelineElement(timelineUtils.buildScheduleRefinement(iun, taxId));
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
