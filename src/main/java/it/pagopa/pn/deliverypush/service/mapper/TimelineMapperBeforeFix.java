package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;

@Slf4j
public class TimelineMapperBeforeFix extends TimelineMapper {

    public void remapSpecificTimelineElementData(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, Instant ingestionTimestamp) {
        if (result != null) {
            //L'ingestion timestamp viene settato con il timestamp originale dell'evento (dunque timestamp evento per SEND)
            result.setIngestionTimestamp(ingestionTimestamp);

            //Nello switch case invece vengono effettuati ulteriori remapping dei timestamp, questi non dipendono dal singolo elemento, ma necessitano di tutta la timeline
            switch (result.getCategory()) {
                case SCHEDULE_REFINEMENT -> {
                    Instant endAnalogWorkflowBusinessDate = computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if (endAnalogWorkflowBusinessDate != null) {
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    }
                }
                case ANALOG_SUCCESS_WORKFLOW, ANALOG_FAILURE_WORKFLOW, COMPLETELY_UNREACHABLE_CREATION_REQUEST, COMPLETELY_UNREACHABLE, ANALOG_WORKFLOW_RECIPIENT_DECEASED -> {
                    Instant endAnalogWorkflowBusinessDate = computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if (endAnalogWorkflowBusinessDate != null) {
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    } else {
                        log.error("SEARCH LAST SEND_ANALOG_FEEDBACK DETAILS NULL element {}", result);
                        throw new PnInternalException("SEND_ANALOG_FEEDBACK NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                    }
                }
                case REFINEMENT -> caseRefinement(timelineElementInternalSet, result);
                case SEND_DIGITAL_DOMICILE -> caseSendDigitalDomicile(timelineElementInternalSet, result);
                default -> {
                    //nothing to do
                }
            }

            //In ultima istanza viene settato l'eventTimestamp con il timestamp rimappato (avranno dunque in uscita sempre lo stesso valore)
            result.setEventTimestamp(result.getTimestamp());
        }
    }

}
