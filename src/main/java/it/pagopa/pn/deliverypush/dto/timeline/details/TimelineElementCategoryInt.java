package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategory.REQUEST_ACCEPTED.getValue()),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class, TimelineElementCategory.SEND_COURTESY_MESSAGE.getValue()),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategory.GET_ADDRESS.getValue()),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class, TimelineElementCategory.PUBLIC_REGISTRY_CALL.getValue()),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE.getValue()),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW.getValue()),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW.getValue()),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class, TimelineElementCategory.SEND_DIGITAL_DOMICILE.getValue()),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategory.SEND_DIGITAL_FEEDBACK.getValue()),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategory.REFINEMENT.getValue()),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategory.SCHEDULE_REFINEMENT.getValue()),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW.getValue()),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW.getValue()),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW.getValue()),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW.getValue()),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER.getValue()),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategory.NOTIFICATION_VIEWED.getValue()),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue()),
    SEND_PAPER_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategory.SEND_PAPER_FEEDBACK.getValue()),
    PAYMENT(null, TimelineElementCategory.PAYMENT.getValue()),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategory.COMPLETELY_UNREACHABLE.getValue()),
    REQUEST_REFUSED(RequestRefusedDetailsInt.class, TimelineElementCategory.REQUEST_REFUSED.getValue()),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategory.AAR_GENERATION.getValue());
    
    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final String value;
    
    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, String value) {
        if(! this.name().equals(value)){
            throw new IllegalArgumentException("Value " +value+" must be equals to enum name "+this.name());
        }
        this.detailsJavaClass = detailsJavaClass;
        this.value = value;
    }
    
    public Class<? extends TimelineElementDetailsInt> getDetailsJavaClass() {
        return this.detailsJavaClass;
    }
    
}
