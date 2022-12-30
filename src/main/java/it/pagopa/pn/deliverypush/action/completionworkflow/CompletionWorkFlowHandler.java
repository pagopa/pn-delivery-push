package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@Slf4j
public class CompletionWorkFlowHandler {
    private final RegisteredLetterSender registeredLetterSender;
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final RefinementScheduler refinementScheduler;
    private final PecDeliveryWorkflowLegalFactsGenerator pecDeliveryWorkflowLegalFactsGenerator;

    public CompletionWorkFlowHandler(RegisteredLetterSender registeredLetterSender,
                                     CompletelyUnreachableUtils completelyUnreachableUtils,
                                     TimelineUtils timelineUtils,
                                     TimelineService timelineService,
                                     MVPParameterConsumer mvpParameterConsumer,
                                     RefinementScheduler refinementScheduler,
                                     PecDeliveryWorkflowLegalFactsGenerator pecDeliveryWorkflowLegalFactsGenerator) {
        this.registeredLetterSender = registeredLetterSender;
        this.completelyUnreachableUtils = completelyUnreachableUtils;
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.refinementScheduler = refinementScheduler;
        this.pecDeliveryWorkflowLegalFactsGenerator = pecDeliveryWorkflowLegalFactsGenerator;
    }


    /**
     * Handle necessary steps to complete the digital workflow
     */
    public void completionDigitalWorkflow(NotificationInt notification, Integer recIndex, Instant completionWorkflowDate, LegalDigitalAddressInt address, EndWorkflowStatus status) {
        log.info("Digital workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);

        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    String legalFactIdSuccess = pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);
                    timelineService.addTimelineElement( timelineUtils.buildSuccessDigitalWorkflowTimelineElement(notification, recIndex, address, legalFactIdSuccess), notification );
                    refinementScheduler.scheduleDigitalRefinement(notification, recIndex, completionWorkflowDate, status);
                    break;
                case FAILURE:
                    String senderTaxId = notification.getSender().getPaTaxId();

                    if( Boolean.FALSE.equals( mvpParameterConsumer.isMvp( senderTaxId ) ) ){
                        String legalFactIdFailure = pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);
                        
                        Instant legalFactGenerationDate = Instant.now();
                        timelineService.addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactIdFailure, legalFactGenerationDate), notification);
                        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, legalFactGenerationDate , status);
                        registeredLetterSender.prepareSimpleRegisteredLetter(notification, recIndex);

                    } else {
                        String legalFactIdFailure = pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);
                        Instant legalFactGenerationDate = Instant.now();
                        timelineService.addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactIdFailure, legalFactGenerationDate), notification);
    
                        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);
                        if( ! isNotificationAlreadyViewed ){
                            log.info("Paper message is not handled, registered Letter will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                            //Qui la notifica è appena passata in DELIVERED e passerà subito in CANCELLED
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
     * Handle necessary steps to complete analog workflow.
     */
    public void completionAnalogWorkflow(NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> attachments, Instant completionWorkflowDate, PhysicalAddressInt usedAddress, EndWorkflowStatus status) {
        log.info("Analog workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS:
                    timelineService.addTimelineElement( timelineUtils.buildSuccessAnalogWorkflowTimelineElement(notification, recIndex, usedAddress, attachments), notification);
                    refinementScheduler.scheduleAnalogRefinement(notification, recIndex, completionWorkflowDate, status);
                    break;
                case FAILURE:
                    timelineService.addTimelineElement( timelineUtils.buildFailureAnalogWorkflowTimelineElement(notification, recIndex, attachments), notification );
                    completelyUnreachableUtils.handleCompletelyUnreachable(notification, recIndex);
                    refinementScheduler.scheduleAnalogRefinement(notification, recIndex, completionWorkflowDate, status);
                    break;
                default:
                    handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
    }
    
    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
    }

    public void addPaperNotificationNotHandledToTimeline(NotificationInt notification, Integer recIndex) {
        timelineService.addTimelineElement(
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
