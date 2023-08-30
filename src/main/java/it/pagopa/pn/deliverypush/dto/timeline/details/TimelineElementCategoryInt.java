package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    SENDER_ACK_CREATION_REQUEST(SenderAckCreationRequestDetailsInt.class, TimelineElementCategory.SENDER_ACK_CREATION_REQUEST.getValue()),
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST(ValidateNormalizeAddressDetailsInt.class, TimelineElementCategory.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.getValue()),
    NORMALIZED_ADDRESS(NormalizedAddressDetailsInt.class, TimelineElementCategory.NORMALIZED_ADDRESS.getValue()),
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategory.REQUEST_ACCEPTED.getValue()),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class, TimelineElementCategory.SEND_COURTESY_MESSAGE.getValue()),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategory.GET_ADDRESS.getValue()),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class, TimelineElementCategory.PUBLIC_REGISTRY_CALL.getValue()),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE.getValue()),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW.getValue()),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW.getValue()),
    PREPARE_DIGITAL_DOMICILE(PrepareDigitalDetailsInt.class, TimelineElementCategory.PREPARE_DIGITAL_DOMICILE.getValue()),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class, TimelineElementCategory.SEND_DIGITAL_DOMICILE.getValue()),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategory.SEND_DIGITAL_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER),
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class, TimelineElementCategory.SEND_DIGITAL_PROGRESS.getValue()),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategory.REFINEMENT.getValue()),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategory.SCHEDULE_REFINEMENT.getValue()),
    DIGITAL_DELIVERY_CREATION_REQUEST(DigitalDeliveryCreationRequestDetailsInt.class, TimelineElementCategory.DIGITAL_DELIVERY_CREATION_REQUEST.getValue()),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW.getValue()),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW.getValue()),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW.getValue()),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW.getValue()),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(CompletelyUnreachableCreationRequestDetails.class, TimelineElementCategory.COMPLETELY_UNREACHABLE_CREATION_REQUEST.getValue()),
    PREPARE_SIMPLE_REGISTERED_LETTER(BaseRegisteredLetterDetailsInt.class, TimelineElementCategory.PREPARE_SIMPLE_REGISTERED_LETTER.getValue()),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER.getValue()),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategory.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue()),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategory.NOTIFICATION_VIEWED.getValue()),
    PREPARE_ANALOG_DOMICILE(BaseAnalogDetailsInt.class, TimelineElementCategory.PREPARE_ANALOG_DOMICILE.getValue()),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategory.SEND_ANALOG_DOMICILE.getValue()),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class, TimelineElementCategory.SEND_ANALOG_PROGRESS.getValue()),
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategory.SEND_ANALOG_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER),
    PAYMENT(NotificationPaidDetailsInt.class, TimelineElementCategory.PAYMENT.getValue()),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategory.COMPLETELY_UNREACHABLE.getValue()),
    REQUEST_REFUSED(RequestRefusedDetailsInt.class, TimelineElementCategory.REQUEST_REFUSED.getValue()),
    AAR_CREATION_REQUEST(AarCreationRequestDetailsInt.class, TimelineElementCategory.AAR_CREATION_REQUEST.getValue()),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategory.AAR_GENERATION.getValue()),
    NOT_HANDLED(NotHandledDetailsInt.class, TimelineElementCategory.NOT_HANDLED.getValue()),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.getValue()),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategory.PROBABLE_SCHEDULING_ANALOG_DATE.getValue()),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategory.NOTIFICATION_CANCELLATION_REQUEST.getValue()),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategory.NOTIFICATION_CANCELLED.getValue());

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
