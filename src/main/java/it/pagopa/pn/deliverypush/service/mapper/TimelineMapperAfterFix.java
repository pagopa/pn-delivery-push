package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;

@Slf4j
public class TimelineMapperAfterFix extends TimelineMapper {

    public void remapSpecificTimelineElementData(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, Instant ingestionTimestamp, boolean isPfNewWorkflowEnabled) {
        if (result != null) {
            //L'ingestion timestamp viene settato con il timestamp originale dell'evento (dunque timestamp evento per SEND)
            result.setIngestionTimestamp(ingestionTimestamp);

            //Nello switch case invece vengono effettuati ulteriori remapping dei timestamp, questi non dipendono dal singolo elemento, ma necessitano di tutta la timeline
            switch (result.getCategory()) {
                case SCHEDULE_REFINEMENT -> {
                    //Se per lo stesso destinatario è presente un elemento di ANALOG_FAILURE_WORKFLOW, viene presa come riferimento la data di questo evento.
                    //In caso contrario, viene presa come riferimento la data dell’evento più recente tra PREPARE_ANALOG_DOMICILE_FAILURE e SEND_ANALOG_FEEDBACK
                    Instant analogFailureWorkflowDate = getAnalogFailureWorkflowDate((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if (analogFailureWorkflowDate != null) {
                        result.setTimestamp(analogFailureWorkflowDate);
                    } else {
                        Instant endAnalogWorkflowBusinessDate = computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                        if (endAnalogWorkflowBusinessDate != null) {
                            log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                            result.setTimestamp(endAnalogWorkflowBusinessDate);
                        }
                    }
                }
                case ANALOG_SUCCESS_WORKFLOW, ANALOG_WORKFLOW_RECIPIENT_DECEASED -> {
                    //Viene presa come riferimento la data dell’evento più recente tra PREPARE_ANALOG_DOMICILE_FAILURE e SEND_ANALOG_FEEDBACK
                    Instant endAnalogWorkflowBusinessDate = computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if (endAnalogWorkflowBusinessDate != null) {
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    } else {
                        log.error("SEARCH LAST SEND_ANALOG_FEEDBACK DETAILS NULL element {}", result);
                        throw new PnInternalException("SEND_ANALOG_FEEDBACK NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                    }
                }
                case COMPLETELY_UNREACHABLE_CREATION_REQUEST, COMPLETELY_UNREACHABLE -> {
                    //Se per lo stesso destinatario è presente un elemento di ANALOG_FAILURE_WORKFLOW, viene presa come riferimento la data di questo evento.
                    Instant analogFailureWorkflowDate = getAnalogFailureWorkflowDate((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if (analogFailureWorkflowDate != null)
                        result.setTimestamp(analogFailureWorkflowDate);
                    else
                        throw new PnInternalException("ANALOG_FAILURE_WORKFLOW NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                }
                case REFINEMENT -> //Viene recuperato per lo stesso destinatario un elemento di tipologia SCHEDULE_REFINEMENT e
                                   //dal dettaglio di quest’ultimo viene estrapolata la data che aveva registrato per l’esecuzione futura del processo di perfezionamento.
                        caseRefinement(timelineElementInternalSet, result);
                case SEND_DIGITAL_DOMICILE -> //Se l’invio della notifica è di tipo SERCQ viene recuperato l’elemento di timeline AAR_GENERATION
                                              //per estrapolare la data in cui è stato effettivamente salvato sulla piattaforma l’atto e restituirla.
                        caseSendDigitalDomicile(timelineElementInternalSet, result, isPfNewWorkflowEnabled);
                case SEND_DIGITAL_FEEDBACK -> caseSendDigitalFeedback(timelineElementInternalSet, result, isPfNewWorkflowEnabled);
                default -> {
                    //nothing to do
                }
            }

            //In ultima istanza viene settato l'eventTimestamp con il timestamp rimappato (avranno dunque in uscita sempre lo stesso valore)
            result.setEventTimestamp(result.getTimestamp());
        }
    }

    private Instant getAnalogFailureWorkflowDate(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet, String iun) {
        if (elementDetails == null) {
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();
        Instant analogFailureWorkflowDate = timelineElementInternalSet.stream()
                .filter(elem ->
                        isElementAnalogFailureWorkflow(elem, recIndex)
                )
                .map(TimelineElementInternal::getTimestamp)
                .findFirst().orElse(null);
        log.debug("analog failure workflow date for iun {} and recIndex {} is {} ", iun, recIndex, analogFailureWorkflowDate);
        return analogFailureWorkflowDate;
    }

    private boolean isElementAnalogFailureWorkflow(TimelineElementInternal elementInternal, Integer recIndex) {
        boolean isValidCategory = (elementInternal.getCategory() == TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW);
        boolean isValidRecIndex = false;
        if (isValidCategory && elementInternal.getDetails() instanceof RecipientRelatedTimelineElementDetails details) {
            isValidRecIndex = details.getRecIndex() == recIndex;
        }
        return isValidRecIndex;
    }

}
