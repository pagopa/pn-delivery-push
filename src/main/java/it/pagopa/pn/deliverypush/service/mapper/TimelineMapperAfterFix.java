package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;

@Slf4j
public class TimelineMapperAfterFix extends TimelineMapper {

    public void remapSpecificTimelineElementData(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, Instant ingestionTimestamp) {
        if(result != null) {
            //L'ingestion timestamp viene settato con il timestamp originale dell'evento (dunque timestamp evento per SEND)
            result.setIngestionTimestamp(ingestionTimestamp);

            //Nello switch case invece vengono effettuati ulteriori remapping dei timestamp, questi non dipendono dal singolo elemento, ma necessitano di tutta la timeline
            switch (result.getCategory()) {
                case SCHEDULE_REFINEMENT -> {
                    Instant endAnalogWorkflowBusinessDate =  computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if(endAnalogWorkflowBusinessDate != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    }
                }
                case ANALOG_SUCCESS_WORKFLOW, ANALOG_FAILURE_WORKFLOW, COMPLETELY_UNREACHABLE_CREATION_REQUEST, COMPLETELY_UNREACHABLE -> {
                    Instant endAnalogWorkflowBusinessDate = computeEndAnalogWorkflowBusinessData((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, result.getIun());
                    if(endAnalogWorkflowBusinessDate != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), endAnalogWorkflowBusinessDate);
                        result.setTimestamp(endAnalogWorkflowBusinessDate);
                    }else{
                        log.error("SEARCH LAST SEND_ANALOG_FEEDBACK DETAILS NULL element {}", result);
                        throw new PnInternalException("SEND_ANALOG_FEEDBACK NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                    }
                }
                case REFINEMENT -> {
                    ScheduleRefinementDetailsInt details = findScheduleRefinementDetails((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet);
                    if(details != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {}", result.getCategory(), result.getTimestamp(), details.getSchedulingDate());
                        result.setTimestamp(details.getSchedulingDate());
                    }else{
                        throw new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
                    }
                }
                case SEND_DIGITAL_DOMICILE -> {
                    // allo scopo di ottenere il timestamp di AAR_GEN per impostare il timestamp di SEND_DIGITAL_DOMICILE, per far si
                    // che il timestamp di SEND_DIGITAL_DOMICILE non sia successivo a quello di SEND_DIGITAL_FEEDBACK per serc SEND
                    //
                    // ottenere channelType e verificare che è sercq
                    // se sercq, ottenere l'address, e se è sercq-send:
                    //  - ottieniamo timestamp di AAR_GEN
                    // - lo usiamo per impostare il timestamp di SEND_DIGITAL_DOMICILE (setTimeStamp)
                    //
                    SendDigitalDetailsInt details = (SendDigitalDetailsInt) result.getDetails();
                    if (LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ.equals(details.getDigitalAddress().getType())) {
                        Instant aarRgenTimestamp = findAARgenTimestamp((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet);
                        result.setTimestamp(aarRgenTimestamp);
                    }
                }
                default -> {
                    //nothing to do
                }
            }

            //In ultima istanza viene settato l'eventTimestamp con il timestamp rimappato (avranno dunque in uscita sempre lo stesso valore)
            result.setEventTimestamp(result.getTimestamp());
        }
    }

}
