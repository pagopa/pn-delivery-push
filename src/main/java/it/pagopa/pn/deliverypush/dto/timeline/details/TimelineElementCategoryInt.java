package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    SENDER_ACK_CREATION_REQUEST(SenderAckCreationRequestDetailsInt.class, TimelineElementCategoryV27.SENDER_ACK_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    VALIDATE_F24_REQUEST(ValidateF24Int.class, DiagnosticTimelineElementCategory.VALIDATE_F24_REQUEST.value, TimelineElementCategoryInt.VERSION_20),
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST(ValidateNormalizeAddressDetailsInt.class, TimelineElementCategoryV27.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.getValue(),TimelineElementCategoryInt.VERSION_10),
    VALIDATED_F24(ValidatedF24DetailInt.class, DiagnosticTimelineElementCategory.VALIDATED_F24.value, TimelineElementCategoryInt.VERSION_20),
    NORMALIZED_ADDRESS(NormalizedAddressDetailsInt.class, TimelineElementCategoryV27.NORMALIZED_ADDRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategoryV27.REQUEST_ACCEPTED.getValue(), TimelineElementCategoryInt.VERSION_10),
    GENERATE_F24_REQUEST(ValidateF24Int.class, DiagnosticTimelineElementCategory.GENERATE_F24_REQUEST.value, TimelineElementCategoryInt.VERSION_23),
    GENERATED_F24(GeneratedF24DetailsInt.class, DiagnosticTimelineElementCategory.GENERATED_F24.value, TimelineElementCategoryInt.VERSION_23),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class, TimelineElementCategoryV27.SEND_COURTESY_MESSAGE.getValue(), TimelineElementCategoryInt.VERSION_10),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategoryV27.GET_ADDRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class, TimelineElementCategoryV27.PUBLIC_REGISTRY_CALL.getValue(), TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class, TimelineElementCategoryV27.PUBLIC_REGISTRY_RESPONSE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class, TimelineElementCategoryV27.SCHEDULE_ANALOG_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class, TimelineElementCategoryV27.SCHEDULE_DIGITAL_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_DIGITAL_DOMICILE(PrepareDigitalDetailsInt.class, TimelineElementCategoryV27.PREPARE_DIGITAL_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class, TimelineElementCategoryV27.SEND_DIGITAL_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategoryV27.SEND_DIGITAL_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER, TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class, TimelineElementCategoryV27.SEND_DIGITAL_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryV27.REFINEMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryV27.SCHEDULE_REFINEMENT.getValue(),TimelineElementCategoryInt.PRIORITY_SCHEDULE_REFINEMENT, TimelineElementCategoryInt.VERSION_10),
    DIGITAL_DELIVERY_CREATION_REQUEST(DigitalDeliveryCreationRequestDetailsInt.class, TimelineElementCategoryV27.DIGITAL_DELIVERY_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategoryV27.DIGITAL_SUCCESS_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategoryV27.DIGITAL_FAILURE_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class, TimelineElementCategoryV27.ANALOG_SUCCESS_WORKFLOW.getValue(),TimelineElementCategoryInt.PRIORITY_ANALOG_SUCCESS_WORKFLOW, TimelineElementCategoryInt.VERSION_10),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class, TimelineElementCategoryV27.ANALOG_FAILURE_WORKFLOW.getValue(),TimelineElementCategoryInt.PRIORITY_ANALOG_FAILURE_WORKFLOW, TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(CompletelyUnreachableCreationRequestDetails.class, TimelineElementCategoryV27.COMPLETELY_UNREACHABLE_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.PRIORITY_COMPLETELY_UNREACHABLE_CREATION_REQUEST, TimelineElementCategoryInt.VERSION_10),
    PREPARE_SIMPLE_REGISTERED_LETTER(BaseRegisteredLetterDetailsInt.class, TimelineElementCategoryV27.PREPARE_SIMPLE_REGISTERED_LETTER.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategoryV27.SEND_SIMPLE_REGISTERED_LETTER.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_VIEWED.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE(BaseAnalogDetailsInt.class, TimelineElementCategoryV27.PREPARE_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE_FAILURE(PrepareAnalogDomicileFailureDetailsInt.class, TimelineElementCategoryV27.PREPARE_ANALOG_DOMICILE_FAILURE.getValue(), TimelineElementCategoryInt.VERSION_20),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_SEND_ANALOG_FEEDBACK, TimelineElementCategoryInt.VERSION_10),
    PAYMENT(NotificationPaidDetailsInt.class, TimelineElementCategoryV27.PAYMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategoryV27.COMPLETELY_UNREACHABLE.getValue(),TimelineElementCategoryInt.PRIORITY_COMPLETELY_UNREACHABLET, TimelineElementCategoryInt.VERSION_10),
    REQUEST_REFUSED(RequestRefusedDetailsInt.class, TimelineElementCategoryV27.REQUEST_REFUSED.getValue(), TimelineElementCategoryInt.VERSION_10),
    AAR_CREATION_REQUEST(AarCreationRequestDetailsInt.class, TimelineElementCategoryV27.AAR_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategoryV27.AAR_GENERATION.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOT_HANDLED(NotHandledDetailsInt.class, TimelineElementCategoryV27.NOT_HANDLED.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class, TimelineElementCategoryV27.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategoryV27.PROBABLE_SCHEDULING_ANALOG_DATE.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_CANCELLATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_CANCELLED.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_RADD_RETRIEVED(NotificationRADDRetrievedDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_RADD_RETRIEVED.getValue(), TimelineElementCategoryInt.VERSION_23),
    NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST(NotificationCancelledDocumentCreationRequestDetailsInt.class, DiagnosticTimelineElementCategory.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST.value, TimelineElementCategoryInt.VERSION_25),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(AnalogWorfklowRecipientDeceasedDetailsInt.class, TimelineElementCategoryV27.ANALOG_WORKFLOW_RECIPIENT_DECEASED.getValue(), TimelineElementCategoryInt.PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED, TimelineElementCategoryInt.VERSION_26),
    PUBLIC_REGISTRY_VALIDATION_CALL(PublicRegistryValidationCallDetailsInt.class, TimelineElementCategoryV27.PUBLIC_REGISTRY_VALIDATION_CALL.getValue(), TimelineElementCategoryInt.VERSION_27),
    PUBLIC_REGISTRY_VALIDATION_RESPONSE(PublicRegistryValidationResponseDetailsInt.class, TimelineElementCategoryV27.PUBLIC_REGISTRY_VALIDATION_RESPONSE.getValue(), TimelineElementCategoryInt.VERSION_27),
    SEND_ANALOG_TIMEOUT_CREATION_REQUEST(SendAnalogTimeoutCreationRequestDetailsInt.class, DiagnosticTimelineElementCategory.SEND_ANALOG_TIMEOUT_CREATION_REQUEST.value, TimelineElementCategoryInt.VERSION_27);


    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final String value;
    private final int priority;
    private final int version;

    public static final int PRIORITY_SEND_ANALOG_FEEDBACK = 30;
    public static final int PRIORITY_ANALOG_SUCCESS_WORKFLOW = 40;
    public static final int PRIORITY_ANALOG_FAILURE_WORKFLOW = 40;
    public static final int PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED = 40;
    public static final int PRIORITY_COMPLETELY_UNREACHABLE_CREATION_REQUEST = 50;
    public static final int PRIORITY_COMPLETELY_UNREACHABLET = 60;
    public static final int PRIORITY_SCHEDULE_REFINEMENT = 70;

    public static final int PRIORITY_BEFORE = 10;
    public static final int PRIORITY_AFTER = 20;

    public static final int VERSION_10 = 10;
    public static final int VERSION_20 = 20;
    public static final int VERSION_23 = 23;
    public static final int VERSION_25 = 25;
    public static final int VERSION_26 = 26;
    public static final int VERSION_27 = 27;

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
        VALIDATE_F24_REQUEST("VALIDATE_F24_REQUEST"),
        GENERATED_F24("GENERATED_F24"),
        GENERATE_F24_REQUEST("GENERATE_F24_REQUEST"),
        NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST("NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST"),
        SEND_ANALOG_TIMEOUT_CREATION_REQUEST("SEND_ANALOG_TIMEOUT_CREATION_REQUEST");

        private final String value;
        DiagnosticTimelineElementCategory(String value) {
            this.value = value;
        }
    }

}
