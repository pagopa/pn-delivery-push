package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;

@Slf4j
public abstract class TimelineMapper {

    public abstract void remapSpecificTimelineElementData(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, Instant ingestionTimestamp);

    Instant findAARgenTimestamp(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet) {
        if (elementDetails == null) {
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        TimelineElementInternal aarGenerationTimelineElement = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.AAR_GENERATION &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails aarGenerationTimelineElementDetails &&
                        aarGenerationTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return aarGenerationTimelineElement.getTimestamp();
    }


    ScheduleRefinementDetailsInt findScheduleRefinementDetails(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet) {
        if (elementDetails == null) {
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        TimelineElementInternal scheduleRefinementTimelineElment = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.SCHEDULE_REFINEMENT &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails scheduleRefinementTimelineElementDetails &&
                        scheduleRefinementTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return (ScheduleRefinementDetailsInt) scheduleRefinementTimelineElment.getDetails();
    }

    Instant computeEndAnalogWorkflowBusinessData(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet, String iun) {
        if (elementDetails == null) {
            throw new PnInternalException("ELEMENT DETAILS NULL", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        Instant endAnalogWorkflowBusinessDate = timelineElementInternalSet.stream().filter(elem ->
                isElementAffectingEndAnalogWorkflowBusinessData(elem, recIndex)
        ).map(this::extractBusinessDate).max(Instant::compareTo).orElse(null);

        log.debug("Business end analog workflow for iun {} is {} ", iun, endAnalogWorkflowBusinessDate);
        return endAnalogWorkflowBusinessDate;
    }

    void caseRefinement(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result) {
        ScheduleRefinementDetailsInt details = findScheduleRefinementDetails((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet);
        if (details != null) {
            log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {}", result.getCategory(), result.getTimestamp(), details.getSchedulingDate());
            result.setTimestamp(details.getSchedulingDate());
        } else {
            throw new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
    }

    void caseSendDigitalDomicile(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result) {
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

    private Instant extractBusinessDate(TimelineElementInternal timelineElementInternal) {
        if (timelineElementInternal.getDetails() instanceof SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails) {
            return sendAnalogFeedbackDetails.getNotificationDate();
        } else if (timelineElementInternal.getDetails() instanceof PrepareAnalogDomicileFailureDetailsInt) {
            return timelineElementInternal.getTimestamp();
        } else {
            throw new PnInternalException("Illegal state: iun " + timelineElementInternal.getIun(), PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

    private boolean isElementAffectingEndAnalogWorkflowBusinessData(TimelineElementInternal elementInternal, Integer recIndex) {
        boolean isValidCategory = (elementInternal.getCategory() == TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK ||
                elementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE_FAILURE);

        boolean isValidRecIndex = false;

        if (isValidCategory && elementInternal.getDetails() instanceof RecipientRelatedTimelineElementDetails details) {
            isValidRecIndex = details.getRecIndex() == recIndex;
        }

        return isValidRecIndex;
    }

}
