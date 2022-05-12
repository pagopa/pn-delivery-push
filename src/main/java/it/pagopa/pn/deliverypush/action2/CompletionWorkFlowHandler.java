package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
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
    private final NotificationUtils notificationUtils;
    private final SchedulerService scheduler;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final TimelineService timelineService;
    private final CompletelyUnreachableUtils completelyUnreachableService;
    private final TimelineUtils timelineUtils;
    private final LegalFactDao legalFactDao;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public CompletionWorkFlowHandler(NotificationUtils notificationUtils, SchedulerService scheduler,
                                     ExternalChannelSendHandler externalChannelSendHandler, TimelineService timelineService,
                                     CompletelyUnreachableUtils completelyUnreachableUtils, TimelineUtils timelineUtils,
                                     LegalFactDao legalFactDao, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.notificationUtils = notificationUtils;
        this.scheduler = scheduler;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.timelineService = timelineService;
        this.completelyUnreachableService = completelyUnreachableUtils;
        this.timelineUtils = timelineUtils;
        this.legalFactDao = legalFactDao;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    /**
     * Handle necessary steps to complete the digital workflow
     */
    public void completionDigitalWorkflow(NotificationInt notification, Integer recIndex, Instant notificationDate, DigitalAddress address, EndWorkflowStatus status) {
        log.info("Digital workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);

        String legalFactId = generatePecDeliveryWorkflowLegalFact(notification, recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement(timelineUtils.buildSuccessDigitalWorkflowTimelineElement(iun, recIndex, address, legalFactId));
                    scheduleRefinement(iun, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessDigitalRefinement());
                    break;
                case FAILURE:
                    sendSimpleRegisteredLetter(notification, recIndex);
                    addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(iun, recIndex, legalFactId));
                    scheduleRefinement(iun, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureDigitalRefinement());
                    break;
                default:
                    handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
    }

    private String generatePecDeliveryWorkflowLegalFact(NotificationInt notification, Integer recIndex) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(notification.getIun());


        List<SendDigitalFeedback> listFeedbackFromExtChannel = timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxId(timelineElement, recIndex))
                .map(timelineElement -> {
                    SendDigitalFeedback sendDigitalFeedback = new SendDigitalFeedback();
                    timelineUtils.getSpecificDetails(timelineElement.getDetails(), sendDigitalFeedback);
                    return sendDigitalFeedback;
                })
                .collect(Collectors.toList());

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return legalFactDao.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient);

    }

    private boolean filterTimelineForTaxId(TimelineElementInternal el, Integer recIndex) {
        boolean availableCategory = TimelineElementCategory.SEND_DIGITAL_FEEDBACK.equals(el.getCategory());
        if (availableCategory) {
            SendDigitalFeedback details = new SendDigitalFeedback();
            timelineUtils.getSpecificDetails(el.getDetails(), details ) ;
            return recIndex.equals(details.getRecIndex());
        }
        return false;
    }

    /**
     * Sent notification by simple registered letter
     */
    private void sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex) {
        //Al termine del workflow digitale se non si è riusciti ad contattare in nessun modo il recipient, viene inviata una raccomanda semplice

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        PhysicalAddress physicalAddress = recipient.getPhysicalAddress();

        if (physicalAddress != null) {
            log.info("Sending simple registered letter  - iun {} id {}", notification.getIun(), recipient.getTaxId());
            externalChannelSendHandler.sendNotificationForRegisteredLetter(notification, physicalAddress, recIndex);
        } else {
            log.info("Simple registered letter can't be send, there isn't physical address for recipient. iun {} id {}", notification.getIun(), recIndex);
        }
    }

    /**
     * Handle necessary steps to complete analog workflow.
     */
    public void completionAnalogWorkflow(NotificationInt notification, Integer recIndex, Instant notificationDate, PhysicalAddress usedAddress, EndWorkflowStatus status) {
        log.info("Analog workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement(timelineUtils.buildSuccessAnalogWorkflowTimelineElement(iun, recIndex, usedAddress));
                    scheduleRefinement(iun, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessAnalogRefinement());
                    break;
                case FAILURE:
                    addTimelineElement(timelineUtils.buildFailureAnalogWorkflowTimelineElement(iun, recIndex));
                    completelyUnreachableService.handleCompletelyUnreachable(notification, recIndex);
                    scheduleRefinement(iun, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureAnalogRefinement());
                    break;
                default:
                    handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
    }

    private void scheduleRefinement(String iun, Integer recIndex, Instant notificationDate, Duration scheduleTime) {
        Instant schedulingDate = notificationDate.plus(scheduleTime);
        log.info("Schedule refinement in {}", schedulingDate);
        
        timelineService.addTimelineElement(timelineUtils.buildScheduleRefinement(iun, recIndex));
        scheduler.scheduleEvent(iun, recIndex, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }


    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex);
    }

    private void addTimelineElement(TimelineElementInternal  element) {
        timelineService.addTimelineElement(element);
    }

}
