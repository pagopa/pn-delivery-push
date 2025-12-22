package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategoryV27.REQUEST_ACCEPTED.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts e costi
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategoryV27.SEND_DIGITAL_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_AFTER, TimelineElementCategoryInt.VERSION_10), //legal facts
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class, TimelineElementCategoryV27.SEND_DIGITAL_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryV27.REFINEMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryV27.SCHEDULE_REFINEMENT.getValue(),TimelineElementCategoryInt.PRIORITY_SCHEDULE_REFINEMENT, TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategoryV27.DIGITAL_SUCCESS_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategoryV27.DIGITAL_FAILURE_WORKFLOW.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategoryV27.SEND_SIMPLE_REGISTERED_LETTER.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_VIEWED.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_FEEDBACK.getValue(), TimelineElementCategoryInt.PRIORITY_SEND_ANALOG_FEEDBACK, TimelineElementCategoryInt.VERSION_10), //legal facts
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategoryV27.COMPLETELY_UNREACHABLE.getValue(),TimelineElementCategoryInt.PRIORITY_COMPLETELY_UNREACHABLET, TimelineElementCategoryInt.VERSION_10), //legal facts
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategoryV27.AAR_GENERATION.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class, TimelineElementCategoryV27.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategoryV27.PROBABLE_SCHEDULING_ANALOG_DATE.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_CANCELLATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_CANCELLED.getValue(), TimelineElementCategoryInt.VERSION_20), //legal facts
    NOTIFICATION_RADD_RETRIEVED(NotificationRADDRetrievedDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_RADD_RETRIEVED.getValue(), TimelineElementCategoryInt.VERSION_23),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(AnalogWorfklowRecipientDeceasedDetailsInt.class, TimelineElementCategoryV27.ANALOG_WORKFLOW_RECIPIENT_DECEASED.getValue(), TimelineElementCategoryInt.PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED, TimelineElementCategoryInt.VERSION_26);


    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final String value;
    private final int priority;
    private final int version;

    public static final int PRIORITY_SEND_ANALOG_FEEDBACK = 30;
    public static final int PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED = 40;
    public static final int PRIORITY_COMPLETELY_UNREACHABLET = 60;
    public static final int PRIORITY_SCHEDULE_REFINEMENT = 70;

    public static final int PRIORITY_BEFORE = 10;
    public static final int PRIORITY_AFTER = 20;

    public static final int VERSION_10 = 10;
    public static final int VERSION_20 = 20;
    public static final int VERSION_23 = 23;
    public static final int VERSION_26 = 26;

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

    /**
     * Checks if the given category is a known TimelineElementCategoryInt.
     *
     * @param category the category to check
     * @return true if the category is known, false otherwise
     */
    public static boolean isKnownCategory(String category) {
        try {
            TimelineElementCategoryInt.valueOf(category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
