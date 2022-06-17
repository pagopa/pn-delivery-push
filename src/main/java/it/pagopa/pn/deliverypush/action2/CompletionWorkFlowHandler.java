package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
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
    private final ExternalChannelService externalChannelService;
    private final TimelineService timelineService;
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final TimelineUtils timelineUtils;
    private final LegalFactDao legalFactDao;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public CompletionWorkFlowHandler(NotificationUtils notificationUtils,
                                     SchedulerService scheduler,
                                     ExternalChannelService externalChannelService,
                                     TimelineService timelineService,
                                     CompletelyUnreachableUtils completelyUnreachableUtils,
                                     TimelineUtils timelineUtils,
                                     LegalFactDao legalFactDao,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.notificationUtils = notificationUtils;
        this.scheduler = scheduler;
        this.externalChannelService = externalChannelService;
        this.timelineService = timelineService;
        this.completelyUnreachableUtils = completelyUnreachableUtils;
        this.timelineUtils = timelineUtils;
        this.legalFactDao = legalFactDao;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    /**
     * Handle necessary steps to complete the digital workflow
     */
    public void completionDigitalWorkflow(NotificationInt notification, Integer recIndex, Instant notificationDate, LegalDigitalAddressInt address, EndWorkflowStatus status) {
        log.info("Digital workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);

        String legalFactId = generatePecDeliveryWorkflowLegalFact(notification, recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement( timelineUtils.buildSuccessDigitalWorkflowTimelineElement(notification, recIndex, address, legalFactId), notification );
                    scheduleRefinement(notification, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessDigitalRefinement());
                    break;
                case FAILURE:
                    sendSimpleRegisteredLetter(notification, recIndex);
                    addTimelineElement( timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId), notification);
                    scheduleRefinement(notification, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureDigitalRefinement());
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
        
        List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel = timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxId(timelineElement, recIndex))
                .map(timelineElement -> 
                   SmartMapper.mapToClass(timelineElement.getDetails(), SendDigitalFeedbackDetailsInt.class))
                .collect(Collectors.toList());

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return legalFactDao.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient);

    }

    private boolean filterTimelineForTaxId(TimelineElementInternal el, Integer recIndex) {
        boolean availableCategory = TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK.equals(el.getCategory());
        if (availableCategory) {
            SendDigitalFeedbackDetailsInt details = SmartMapper.mapToClass(el.getDetails(), SendDigitalFeedbackDetailsInt.class);
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
        PhysicalAddressInt physicalAddress = recipient.getPhysicalAddress();

        if (physicalAddress != null) {
            log.info("Sending simple registered letter  - iun {} id {}", notification.getIun(), recIndex);
            externalChannelService.sendNotificationForRegisteredLetter(notification, physicalAddress, recIndex);
        } else {
            log.info("Simple registered letter can't be send, there isn't physical address for recipient. iun {} id {}", notification.getIun(), recIndex);
        }
    }

    /**
     * Handle necessary steps to complete analog workflow.
     */
    public void completionAnalogWorkflow(NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> attachments, Instant notificationDate, PhysicalAddressInt usedAddress, EndWorkflowStatus status) {
        log.info("Analog workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    addTimelineElement( timelineUtils.buildSuccessAnalogWorkflowTimelineElement(notification, recIndex, usedAddress, attachments), notification);
                    scheduleRefinement(notification, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessAnalogRefinement());
                    break;
                case FAILURE:
                    addTimelineElement( timelineUtils.buildFailureAnalogWorkflowTimelineElement(notification, recIndex, attachments), notification );
                    completelyUnreachableUtils.handleCompletelyUnreachable(notification, recIndex);
                    scheduleRefinement(notification, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureAnalogRefinement());
                    break;
                default:
                    handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
    }

    private void scheduleRefinement(NotificationInt notification, Integer recIndex, Instant notificationDate, Duration scheduleTime) {
        Instant schedulingDate = notificationDate.plus(scheduleTime);
        log.info("Schedule refinement in {}", schedulingDate);

        addTimelineElement( timelineUtils.buildScheduleRefinement(notification, recIndex), notification );
        scheduler.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }

    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
