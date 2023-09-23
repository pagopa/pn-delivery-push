package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV20;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    SENDER_ACK_CREATION_REQUEST(SenderAckCreationRequestDetailsInt.class, TimelineElementCategoryV20.SENDER_ACK_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    VALIDATE_F24_REQUEST(ValidateF24Int.class, DiagnosticTimelineElementCategory.VALIDATE_F24_REQUEST.value, TimelineElementCategoryInt.VERSION_20),
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST(ValidateNormalizeAddressDetailsInt.class, TimelineElementCategoryV20.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.getValue(),TimelineElementCategoryInt.VERSION_10),
    VALIDATED_F24(ValidatedF24DetailInt.class, DiagnosticTimelineElementCategory.VALIDATED_F24.value, TimelineElementCategoryInt.VERSION_20),
    NORMALIZED_ADDRESS(NormalizedAddressDetailsInt.class, TimelineElementCategoryV20.NORMALIZED_ADDRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategoryV20.REQUEST_ACCEPTED.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class, TimelineElementCategoryV20.SEND_COURTESY_MESSAGE.getValue(), TimelineElementCategoryInt.VERSION_10),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategoryV20.GET_ADDRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class, TimelineElementCategoryV20.PUBLIC_REGISTRY_CALL.getValue(), TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class, TimelineElementCategoryV20.PUBLIC_REGISTRY_RESPONSE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class, TimelineElementCategoryV20.SCHEDULE_ANALOG_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class, TimelineElementCategoryV20.SCHEDULE_DIGITAL_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_DIGITAL_DOMICILE(PrepareDigitalDetailsInt.class, TimelineElementCategoryV20.PREPARE_DIGITAL_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class, TimelineElementCategoryV20.SEND_DIGITAL_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategoryV20.SEND_DIGITAL_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER, TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class, TimelineElementCategoryV20.SEND_DIGITAL_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryV20.REFINEMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryV20.SCHEDULE_REFINEMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    DIGITAL_DELIVERY_CREATION_REQUEST(DigitalDeliveryCreationRequestDetailsInt.class, TimelineElementCategoryV20.DIGITAL_DELIVERY_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategoryV20.DIGITAL_SUCCESS_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategoryV20.DIGITAL_FAILURE_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class, TimelineElementCategoryV20.ANALOG_SUCCESS_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class, TimelineElementCategoryV20.ANALOG_FAILURE_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(CompletelyUnreachableCreationRequestDetails.class, TimelineElementCategoryV20.COMPLETELY_UNREACHABLE_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_SIMPLE_REGISTERED_LETTER(BaseRegisteredLetterDetailsInt.class, TimelineElementCategoryV20.PREPARE_SIMPLE_REGISTERED_LETTER.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategoryV20.SEND_SIMPLE_REGISTERED_LETTER.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_VIEWED.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE(BaseAnalogDetailsInt.class, TimelineElementCategoryV20.PREPARE_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE_FAILURE(PrepareAnalogDomicileFailureDetailsInt.class, TimelineElementCategoryV20.PREPARE_ANALOG_DOMICILE_FAILURE.getValue(), TimelineElementCategoryInt.VERSION_20),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryV20.SEND_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class, TimelineElementCategoryV20.SEND_ANALOG_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategoryV20.SEND_ANALOG_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER, TimelineElementCategoryInt.VERSION_10),
    PAYMENT(NotificationPaidDetailsInt.class, TimelineElementCategoryV20.PAYMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategoryV20.COMPLETELY_UNREACHABLE.getValue(), TimelineElementCategoryInt.VERSION_10),
    REQUEST_REFUSED(RequestRefusedDetailsInt.class, TimelineElementCategoryV20.REQUEST_REFUSED.getValue(), TimelineElementCategoryInt.VERSION_10),
    AAR_CREATION_REQUEST(AarCreationRequestDetailsInt.class, TimelineElementCategoryV20.AAR_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategoryV20.AAR_GENERATION.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOT_HANDLED(NotHandledDetailsInt.class, TimelineElementCategoryV20.NOT_HANDLED.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class, TimelineElementCategoryV20.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategoryV20.PROBABLE_SCHEDULING_ANALOG_DATE.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_CANCELLATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryV20.NOTIFICATION_CANCELLED.getValue(), TimelineElementCategoryInt.VERSION_20);

    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final String value;
    private final int priority;
    private final int version;

    public static final int PRIORITY_BEFORE = 10;
    public static final int PRIORITY_AFTER = 20;

    public static final int VERSION_10 = 10;
    public static final int VERSION_20 = 20;

    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, String value, int version) {
        this(detailsJavaClass, value, PRIORITY_BEFORE, version);
    }


    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, String value, int priority, int version) {
        if(! this.name().equals(value)){
            throw new IllegalArgumentException("Value " +value+" must be equals to enum name "+this.name());
        }
        this.detailsJavaClass = detailsJavaClass;
        this.value = value;
        this.priority = priority;
        this.version = version;
    }
    
    public Class<? extends TimelineElementDetailsInt> getDetailsJavaClass() {
        return this.detailsJavaClass;
    }

    public enum DiagnosticTimelineElementCategory {
        VALIDATED_F24("VALIDATED_F24"),
        VALIDATE_F24_REQUEST("VALIDATE_F24_REQUEST");

        private final String value;
        DiagnosticTimelineElementCategory(String value) {
            this.value = value;
        }
    }
    
}
