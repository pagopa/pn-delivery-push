package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogDeliveryFailureWorkflowLegalFactsGenerator;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@AllArgsConstructor
@CustomLog
public class CompletionWorkFlowHandler {
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final RefinementScheduler refinementScheduler;
    private final PecDeliveryWorkflowLegalFactsGenerator pecDeliveryWorkflowLegalFactsGenerator;
    private final AnalogDeliveryFailureWorkflowLegalFactsGenerator analogDeliveryFailureWorkflowLegalFactsGenerator;
    private final DocumentCreationRequestService documentCreationRequestService;
    private final FailureWorkflowHandler failureWorkflowHandler;
    
    //Complete digital failure workflow first step
    public void sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex) {
        log.info("Digital workflow completed with status{}. Sending simple registered letter - IUN {} id {}", EndWorkflowStatus.FAILURE, notification.getIun(), recIndex);
        failureWorkflowHandler.sendSimpleRegisteredLetter(notification, recIndex);
    }
    
    //Complete digital failure workflow second step
    public void completionDigitalFailureWorkflow(NotificationInt notification, Integer recIndex) {
        log.info("Digital workflow failed. Proceed to generate legalFact - IUN {} id {}", notification.getIun(), recIndex);

        Instant completionWorkflowDate = Instant.now();
        String legalFactId = pecDeliveryWorkflowLegalFactsGenerator.generateAndSendCreationRequestForPecDeliveryWorkflowLegalFact(
                notification,
                recIndex,
                EndWorkflowStatus.FAILURE, 
                completionWorkflowDate
        );

        TimelineElementInternal timelineElementInternal = timelineUtils.buildDigitalDeliveryLegalFactCreationRequestTimelineElement(notification, recIndex,  EndWorkflowStatus.FAILURE, 
                completionWorkflowDate, null, legalFactId);
        boolean timelineInsertSkipped = timelineService.addTimelineElement(timelineElementInternal, notification);

        if(timelineInsertSkipped){
            //Se l'elemento di timeline è stato inserito in precedenza, la data di completionWorkflow da utilizzare dovrà essere quella dell'elemento di timeline già presente.
            completionWorkflowDate = getCompletionWorkflowDate(notification, completionWorkflowDate, timelineElementInternal);
        }

        //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
        documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.DIGITAL_DELIVERY, timelineElementInternal.getElementId());
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, completionWorkflowDate, EndWorkflowStatus.FAILURE);
    }


    private Instant getCompletionWorkflowDate(NotificationInt notification, Instant completionWorkflowDate, TimelineElementInternal timelineElementInternal) {
        Optional<DigitalDeliveryCreationRequestDetailsInt> alreadyInsertedTimelineElementOpt = timelineService.getTimelineElementDetails(notification.getIun(), timelineElementInternal.getElementId(), DigitalDeliveryCreationRequestDetailsInt.class);
        if(alreadyInsertedTimelineElementOpt.isPresent()){
            DigitalDeliveryCreationRequestDetailsInt alreadyInsertedTimelineElement = alreadyInsertedTimelineElementOpt.get();
            completionWorkflowDate = alreadyInsertedTimelineElement.getCompletionWorkflowDate();
        }
        return completionWorkflowDate;
    }


    /**
     * Handle necessary steps to complete the digital workflow
     */
    public String completionSuccessDigitalWorkflow(NotificationInt notification, Integer recIndex, Instant completionWorkflowDate, LegalDigitalAddressInt address) {
        log.info("Digital workflow completed with status {} IUN {} id {}", EndWorkflowStatus.SUCCESS, notification.getIun(), recIndex);
        String legalFactId = pecDeliveryWorkflowLegalFactsGenerator.generateAndSendCreationRequestForPecDeliveryWorkflowLegalFact(notification, recIndex, EndWorkflowStatus.SUCCESS, completionWorkflowDate);

        TimelineElementInternal timelineElementInternal = timelineUtils.buildDigitalDeliveryLegalFactCreationRequestTimelineElement(notification, recIndex,  EndWorkflowStatus.SUCCESS, completionWorkflowDate, address, legalFactId);
        timelineService.addTimelineElement(timelineElementInternal, notification);
        
        //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
        documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.DIGITAL_DELIVERY, timelineElementInternal.getElementId());

        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, completionWorkflowDate, EndWorkflowStatus.SUCCESS);
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
                    AarGenerationDetailsInt aarGenerationDetails = retrieveAARTimelineElement(iun, recIndex);
                    
                    TimelineElementInternal failureAnalogWorkflow = timelineUtils.buildFailureAnalogWorkflowTimelineElement(notification, recIndex, aarGenerationDetails.getGeneratedAarUrl());
                    timelineService.addTimelineElement(failureAnalogWorkflow, notification);
                    
                    String legalFactId = analogDeliveryFailureWorkflowLegalFactsGenerator.generateAndSendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(notification, recIndex, status, failureAnalogWorkflow.getTimestamp());

                    TimelineElementInternal timelineElementInternal = timelineUtils.buildAnalogDeliveryFailedLegalFactCreationRequestTimelineElement(notification, recIndex, status, completionWorkflowDate, legalFactId);
                    timelineService.addTimelineElement(timelineElementInternal, notification);

                    //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
                    documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.ANALOG_FAILURE_DELIVERY, timelineElementInternal.getElementId());
                }
                default -> handleError(iun, recIndex, status);
            }
        } else {
            handleError(iun, recIndex, null);
        }
    }

    private AarGenerationDetailsInt retrieveAARTimelineElement(String iun, Integer recIndex) {
        Optional<AarGenerationDetailsInt> aarGenerationDetailsOpt = timelineService.getTimelineElementDetailForSpecificRecipient(iun, recIndex, false, TimelineElementCategoryInt.AAR_GENERATION, AarGenerationDetailsInt.class);
        
        if (aarGenerationDetailsOpt.isPresent()) {
            log.info("retrieveAARTimestampFromTimeline iun={} recIndex={}", iun, recIndex);
            return aarGenerationDetailsOpt.get();
        }
        else
        {
            log.fatal("Cannot retrieve AAR generation for iun={} recIndex={}", iun, recIndex);
            throw new PnInternalException("Cannot retrieve AAR generation for Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
        }
    }


    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status) {
        log.error("Specified status {} does not exist. iun={} recIndex={}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
    }

}
