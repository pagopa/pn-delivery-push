package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class CompletionWorkFlowHandler {
    private final NotificationUtils notificationUtils;
    private final SchedulerService scheduler;
    private final ExternalChannelService externalChannelService;
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final TimelineUtils timelineUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final CompletionWorkflowUtils completionWorkflowUtils;
    
    public CompletionWorkFlowHandler(NotificationUtils notificationUtils,
                                     SchedulerService scheduler,
                                     ExternalChannelService externalChannelService,
                                     CompletelyUnreachableUtils completelyUnreachableUtils,
                                     TimelineUtils timelineUtils,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs, 
                                     CompletionWorkflowUtils completionWorkflowUtils
    ) {
        this.notificationUtils = notificationUtils;
        this.scheduler = scheduler;
        this.externalChannelService = externalChannelService;
        this.completelyUnreachableUtils = completelyUnreachableUtils;
        this.timelineUtils = timelineUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.completionWorkflowUtils = completionWorkflowUtils;
    }

    /**
     * Handle necessary steps to complete the digital workflow
     */
    public void completionDigitalWorkflow(NotificationInt notification, Integer recIndex, Instant completionWorkflowDate, LegalDigitalAddressInt address, EndWorkflowStatus status) {
        log.info("Digital workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);

        String legalFactId = completionWorkflowUtils.generatePecDeliveryWorkflowLegalFact(notification, recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    completionWorkflowUtils.addTimelineElement( timelineUtils.buildSuccessDigitalWorkflowTimelineElement(notification, recIndex, address, legalFactId), notification );
                    scheduleRefinement(notification, recIndex, completionWorkflowDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessDigitalRefinement());
                    break;
                case FAILURE:
                    if( Boolean.FALSE.equals( pnDeliveryPushConfigs.getPaperMessageNotHandled()) ){
                        sendSimpleRegisteredLetter(notification, recIndex);
                        completionWorkflowUtils.addTimelineElement( timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId), notification);
                        scheduleRefinement(notification, recIndex, completionWorkflowDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureDigitalRefinement());
                        
                    }else {
                        completionWorkflowUtils.addTimelineElement( timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId), notification );

                        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

                        if( ! isNotificationAlreadyViewed ){
                            log.info("Paper message is not handled, registered Letter will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                            addPaperNotificationNotHandledToTimeline(notification, recIndex);
                        } else {
                            log.info("Notification is already viewed, it will not go into the cancelled state - iun={} recipientIndex={}", notification.getIun(), recIndex);
                        }
                    }
                    break;
                default:
                    handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
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
                    completionWorkflowUtils.addTimelineElement( timelineUtils.buildSuccessAnalogWorkflowTimelineElement(notification, recIndex, usedAddress, attachments), notification);
                    scheduleRefinement(notification, recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessAnalogRefinement());
                    break;
                case FAILURE:
                    completionWorkflowUtils.addTimelineElement( timelineUtils.buildFailureAnalogWorkflowTimelineElement(notification, recIndex, attachments), notification );
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

    private void scheduleRefinement(NotificationInt notification, Integer recIndex, Instant notificationDate, Duration schedulingDays) {
        log.info("Start scheduling refinement - iun={} id={}", notification.getIun(), recIndex);
        
        Instant schedulingDate = completionWorkflowUtils.getSchedulingDate(notificationDate, schedulingDays, notification.getIun());
        
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        //Se la notifica è già stata visualizzata, non viene schedulato il perfezionamento per decorrenza termini dal momento che la notifica è già stata perfezionata per visione
        if( !isNotificationAlreadyViewed ){
            log.info("Schedule refinement in date={} - iun={} id={}", schedulingDate, notification.getIun(), recIndex);
            completionWorkflowUtils.addTimelineElement( timelineUtils.buildScheduleRefinement(notification, recIndex), notification );
            scheduler.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
        }else {
            log.info("Notification is already viewed, refinement will not be scheduled - iun={} id={}", notification.getIun(), recIndex);
        }
    }
    
    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex);
    }

    public void addPaperNotificationNotHandledToTimeline(NotificationInt notification, Integer recIndex) {
        completionWorkflowUtils.addTimelineElement(
                timelineUtils.buildNotHandledTimelineElement(
                        notification,
                        recIndex,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_CODE,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_REASON
                ),
                notification
        );
    }
}
