package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV20;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    SENDER_ACK_CREATION_REQUEST(SenderAckCreationRequestDetailsInt.class, TimelineElementCategoryV20.SENDER_ACK_CREATION_REQUEST.getValue()),
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST(ValidateNormalizeAddressDetailsInt.class, TimelineElementCategoryV20.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.getValue()),
    NORMALIZED_ADDRESS(NormalizedAddressDetailsInt.class, TimelineElementCategoryV20.NORMALIZED_ADDRESS.getValue()),
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategoryV20.REQUEST_ACCEPTED.getValue()),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class, TimelineElementCategoryV20.SEND_COURTESY_MESSAGE.getValue()),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategoryV20.GET_ADDRESS.getValue()),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class, TimelineElementCategoryV20.PUBLIC_REGISTRY_CALL.getValue()),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class, TimelineElementCategoryV20.PUBLIC_REGISTRY_RESPONSE.getValue()),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class, TimelineElementCategoryV20.SCHEDULE_ANALOG_WORKFLOW.getValue()),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class, TimelineElementCategoryV20.SCHEDULE_DIGITAL_WORKFLOW.getValue()),
    PREPARE_DIGITAL_DOMICILE(PrepareDigitalDetailsInt.class, TimelineElementCategoryV20.PREPARE_DIGITAL_DOMICILE.getValue()),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class, TimelineElementCategoryV20.SEND_DIGITAL_DOMICILE.getValue()),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategoryV20.SEND_DIGITAL_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER),
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class, TimelineElementCategoryV20.SEND_DIGITAL_PROGRESS.getValue()),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryV20.REFINEMENT.getValue()),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryV20.SCHEDULE_REFINEMENT.getValue()),
    DIGITAL_DELIVERY_CREATION_REQUEST(DigitalDeliveryCreationRequestDetailsInt.class, TimelineElementCategoryV20.DIGITAL_DELIVERY_CREATION_REQUEST.getValue()),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategoryV20.DIGITAL_SUCCESS_WORKFLOW.getValue()),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategoryV20.DIGITAL_FAILURE_WORKFLOW.getValue()),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class, TimelineElementCategoryV20.ANALOG_SUCCESS_WORKFLOW.getValue()),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class, TimelineElementCategoryV20.ANALOG_FAILURE_WORKFLOW.getValue()),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(CompletelyUnreachableCreationRequestDetails.class, TimelineElementCategoryV20.COMPLETELY_UNREACHABLE_CREATION_REQUEST.getValue()),
    PREPARE_SIMPLE_REGISTERED_LETTER(BaseRegisteredLetterDetailsInt.class, TimelineElementCategoryV20.PREPARE_SIMPLE_REGISTERED_LETTER.getValue()),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategoryV20.SEND_SIMPLE_REGISTERED_LETTER.getValue()),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue()),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_VIEWED.getValue()),
    PREPARE_ANALOG_DOMICILE(BaseAnalogDetailsInt.class, TimelineElementCategoryV20.PREPARE_ANALOG_DOMICILE.getValue()),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryV20.SEND_ANALOG_DOMICILE.getValue()),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class, TimelineElementCategoryV20.SEND_ANALOG_PROGRESS.getValue()),
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategoryV20.SEND_ANALOG_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER),
    PAYMENT(NotificationPaidDetailsInt.class, TimelineElementCategoryV20.PAYMENT.getValue()),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategoryV20.COMPLETELY_UNREACHABLE.getValue()),
    REQUEST_REFUSED(RequestRefusedDetailsInt.class, TimelineElementCategoryV20.REQUEST_REFUSED.getValue()),
    AAR_CREATION_REQUEST(AarCreationRequestDetailsInt.class, TimelineElementCategoryV20.AAR_CREATION_REQUEST.getValue()),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategoryV20.AAR_GENERATION.getValue()),
    NOT_HANDLED(NotHandledDetailsInt.class, TimelineElementCategoryV20.NOT_HANDLED.getValue()),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class, TimelineElementCategoryV20.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.getValue()),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategoryV20.PROBABLE_SCHEDULING_ANALOG_DATE.getValue()),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_CANCELLATION_REQUEST.getValue()),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_CANCELLED.getValue());

    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final String value;
    private final int priority;


    public static final int PRIORITY_BEFORE = 10;
    public static final int PRIORITY_AFTER = 20;
    
    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, String value) {
        this(detailsJavaClass, value, PRIORITY_BEFORE);
    }


    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, String value, int priority) {
        if(! this.name().equals(value)){
            throw new IllegalArgumentException("Value " +value+" must be equals to enum name "+this.name());
        }
        this.detailsJavaClass = detailsJavaClass;
        this.value = value;
        this.priority = priority;
    }
    
    public Class<? extends TimelineElementDetailsInt> getDetailsJavaClass() {
        return this.detailsJavaClass;
    }
    
}
