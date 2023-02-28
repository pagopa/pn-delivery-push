package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class CompletionWorkFlowHandler {
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final RefinementScheduler refinementScheduler;
    private final PecDeliveryWorkflowLegalFactsGenerator pecDeliveryWorkflowLegalFactsGenerator;
    private final DocumentCreationRequestService documentCreationRequestService;

    /**
     * Handle necessary steps to complete the digital workflow
     */
    public String completionDigitalWorkflow(NotificationInt notification, Integer recIndex, Instant completionWorkflowDate, LegalDigitalAddressInt address, EndWorkflowStatus status) {
        log.info("Digital workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);
        String legalFactId = pecDeliveryWorkflowLegalFactsGenerator.generateAndSendCreationRequestForPecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);

        TimelineElementInternal timelineElementInternal = timelineUtils.buildDigitalDeliveryLegalFactCreationRequestTimelineElement(notification, recIndex,  status, completionWorkflowDate, address, legalFactId);
        timelineService.addTimelineElement(timelineElementInternal, notification);

        //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
        documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.DIGITAL_DELIVERY, timelineElementInternal.getElementId());
        
        return timelineElementInternal.getElementId();
    }
    
    /**
     * Handle necessary steps to complete analog workflow.
     */
    public void completionAnalogWorkflow(NotificationInt notification, Integer recIndex, Instant completionWorkflowDate, PhysicalAddressInt usedAddress, EndWorkflowStatus status) {
        log.info("Analog workflow completed with status {} IUN {} id {}", status, notification.getIun(), recIndex);
        String iun = notification.getIun();
        
        if (status != null) {
            switch (status) {
                case SUCCESS -> {
                    timelineService.addTimelineElement(timelineUtils.buildSuccessAnalogWorkflowTimelineElement(notification, recIndex, usedAddress), notification);
                    refinementScheduler.scheduleAnalogRefinement(notification, recIndex, completionWorkflowDate, status);
                }
                case FAILURE -> {
                    timelineService.addTimelineElement(timelineUtils.buildFailureAnalogWorkflowTimelineElement(notification, recIndex), notification);
                    completelyUnreachableUtils.handleCompletelyUnreachable(notification, recIndex);
                    refinementScheduler.scheduleAnalogRefinement(notification, recIndex, completionWorkflowDate, status);
                }
                default -> handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
    }
    
    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
    }
}
