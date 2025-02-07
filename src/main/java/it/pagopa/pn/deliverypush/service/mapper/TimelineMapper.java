package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Slf4j
public abstract class TimelineMapper {

    public static final String ELEMENT_DETAILS_NULL = "ELEMENT DETAILS NULL";
    public static final String SCHEDULE_REFINEMENT_NOT_PRESENT_ERROR_IN_MAPPING = "SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING";

    public abstract void remapSpecificTimelineElementData(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, Instant ingestionTimestamp, boolean isPfNewWorkflowEnabled);

    Instant findAARgenTimestamp(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet) {
        if (elementDetails == null) {
            throw new PnInternalException(ELEMENT_DETAILS_NULL, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        TimelineElementInternal aarGenerationTimelineElement = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.AAR_GENERATION &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails aarGenerationTimelineElementDetails &&
                        aarGenerationTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException(SCHEDULE_REFINEMENT_NOT_PRESENT_ERROR_IN_MAPPING, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return aarGenerationTimelineElement.getTimestamp();
    }

    private Instant findSendDigitalTimestamp(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementCategoryInt timelineElementCategoryInt) {
        if (elementDetails == null) {
            throw new PnInternalException(ELEMENT_DETAILS_NULL, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        return timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == timelineElementCategoryInt &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails sendDigitalDomicileElementDetails &&
                        sendDigitalDomicileElementDetails.getRecIndex() == recIndex
        ).findFirst().map(TimelineElementInternal::getTimestamp).orElse(null);
    }

    ScheduleRefinementDetailsInt findScheduleRefinementDetails(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet) {
        if (elementDetails == null) {
            throw new PnInternalException(ELEMENT_DETAILS_NULL, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
        int recIndex = elementDetails.getRecIndex();

        TimelineElementInternal scheduleRefinementTimelineElment = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.SCHEDULE_REFINEMENT &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails scheduleRefinementTimelineElementDetails &&
                        scheduleRefinementTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException(SCHEDULE_REFINEMENT_NOT_PRESENT_ERROR_IN_MAPPING, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return (ScheduleRefinementDetailsInt) scheduleRefinementTimelineElment.getDetails();
    }

    Instant computeEndAnalogWorkflowBusinessData(RecipientRelatedTimelineElementDetails elementDetails, Set<TimelineElementInternal> timelineElementInternalSet, String iun) {
        if (elementDetails == null) {
            throw new PnInternalException(ELEMENT_DETAILS_NULL, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
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
            throw new PnInternalException(SCHEDULE_REFINEMENT_NOT_PRESENT_ERROR_IN_MAPPING, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
    }

    void caseSendDigitalDomicile(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, boolean isPfNewWorkflowEnabled) {
        // allo scopo di ottenere il timestamp di AAR_GEN per impostare il timestamp di SEND_DIGITAL_DOMICILE, per far si
        // che il timestamp di SEND_DIGITAL_DOMICILE non sia successivo a quello di SEND_DIGITAL_FEEDBACK per serc SEND solo in caso di vecchio
        // workflow di recupero domicili digitali
        //
        // ottenere channelType e verificare che è sercq
        // se sercq, ottenere l'address, e se è sercq-send:
        //  - ottieniamo timestamp di AAR_GEN
        // - lo usiamo per impostare il timestamp di SEND_DIGITAL_DOMICILE (setTimeStamp)
        //
        SendDigitalDetailsInt details = (SendDigitalDetailsInt) result.getDetails();
        boolean isSercq = LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ.equals(details.getDigitalAddress().getType());
        if (!isPfNewWorkflowEnabled && isSercq) {
            Instant aarRgenTimestamp = findAARgenTimestamp((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet);
            result.setTimestamp(aarRgenTimestamp);
        } else if (isPfNewWorkflowEnabled && isSercq){
            setDigitalDomicile(timelineElementInternalSet, result);
        }
    }

    private void setDigitalDomicile(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result) {
        Instant sendDigitalFeedbackTimestamp =  findSendDigitalTimestamp((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK);
        if(Objects.nonNull(sendDigitalFeedbackTimestamp) && sendDigitalFeedbackTimestamp.isBefore(result.getIngestionTimestamp())){
            result.setTimestamp(sendDigitalFeedbackTimestamp);
            result.setEventTimestamp(sendDigitalFeedbackTimestamp);
        }else {
            result.setTimestamp(result.getIngestionTimestamp());
            result.setEventTimestamp(result.getIngestionTimestamp());
        }
    }

    void caseSendDigitalFeedback(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result, boolean isPfNewWorkflowEnabled) {
        //caso implementato solo per la gestione del digital feedback in caso di SERCQ e nuovo workflow di recupero domicili digitali attivo
        SendDigitalFeedbackDetailsInt details = (SendDigitalFeedbackDetailsInt) result.getDetails();
        if (isPfNewWorkflowEnabled && LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ.equals(details.getDigitalAddress().getType())) {
            setDigitalFeedbackTimestamp(timelineElementInternalSet, result);
        }
    }

    private void setDigitalFeedbackTimestamp(Set<TimelineElementInternal> timelineElementInternalSet, TimelineElementInternal result) {
        Instant sendDigitalDomicileTimestamp =  findSendDigitalTimestamp((RecipientRelatedTimelineElementDetails) result.getDetails(), timelineElementInternalSet, TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE);
        if(Objects.nonNull(sendDigitalDomicileTimestamp) && sendDigitalDomicileTimestamp.isAfter(result.getIngestionTimestamp())){
            result.setTimestamp(sendDigitalDomicileTimestamp);
            result.setEventTimestamp(sendDigitalDomicileTimestamp);
        }else {
            result.setTimestamp(result.getIngestionTimestamp());
            result.setEventTimestamp(result.getIngestionTimestamp());
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
