package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategoryV28.REQUEST_ACCEPTED.getValue(), TimelineElementCategoryInt.VERSION_10), //legal facts e costi
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryV28.REFINEMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryV28.SCHEDULE_REFINEMENT.getValue(),TimelineElementCategoryInt.PRIORITY_SCHEDULE_REFINEMENT, TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryV28.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategoryV28.SEND_SIMPLE_REGISTERED_LETTER.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryV28.NOTIFICATION_VIEWED.getValue(), TimelineElementCategoryInt.VERSION_10), //visualizzazione
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryV28.SEND_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryV28.NOTIFICATION_CANCELLED.getValue(), TimelineElementCategoryInt.VERSION_20), //legal facts (Deve rimanere per TimelineUtils.checkIsNotificationCancelledLegalFactId)
    NOTIFICATION_RADD_RETRIEVED(NotificationRADDRetrievedDetailsInt.class, TimelineElementCategoryV28.NOTIFICATION_RADD_RETRIEVED.getValue(), TimelineElementCategoryInt.VERSION_23),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(AnalogWorfklowRecipientDeceasedDetailsInt.class, TimelineElementCategoryV28.ANALOG_WORKFLOW_RECIPIENT_DECEASED.getValue(), TimelineElementCategoryInt.PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED, TimelineElementCategoryInt.VERSION_26);


    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final String value;
    private final int priority;
    private final int version;

    public static final int PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED = 40;
    public static final int PRIORITY_SCHEDULE_REFINEMENT = 70;

    public static final int PRIORITY_BEFORE = 10;

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
