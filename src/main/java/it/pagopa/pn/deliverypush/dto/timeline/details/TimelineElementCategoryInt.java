package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    NORMALIZED_ADDRESS(NormalizedAddressDetailsInt.class, TimelineElementCategoryV27.NORMALIZED_ADDRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    REQUEST_ACCEPTED(NotificationRequestAcceptedDetailsInt.class, TimelineElementCategoryV27.REQUEST_ACCEPTED.getValue(), TimelineElementCategoryInt.VERSION_10),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryV27.REFINEMENT.getValue(), TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryV27.SCHEDULE_REFINEMENT.getValue(),TimelineElementCategoryInt.PRIORITY_SCHEDULE_REFINEMENT, TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryV27.SEND_ANALOG_DOMICILE.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE_FAILURE(PrepareAnalogDomicileFailureDetailsInt.class, TimelineElementCategoryV27.PREPARE_ANALOG_DOMICILE_FAILURE.getValue(), TimelineElementCategoryInt.VERSION_20),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategoryV27.AAR_GENERATION.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_VIEWED.getValue(), TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_CANCELLATION_REQUEST(NotificationCancellationRequestDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_CANCELLATION_REQUEST.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_CANCELLED.getValue(), TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_RADD_RETRIEVED(NotificationRADDRetrievedDetailsInt.class, TimelineElementCategoryV27.NOTIFICATION_RADD_RETRIEVED.getValue(), TimelineElementCategoryInt.VERSION_23),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategoryV27.GET_ADDRESS.getValue(), TimelineElementCategoryInt.VERSION_10),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategoryV27.PROBABLE_SCHEDULING_ANALOG_DATE.getValue(), TimelineElementCategoryInt.VERSION_20),    ANALOG_WORKFLOW_RECIPIENT_DECEASED(AnalogWorfklowRecipientDeceasedDetailsInt.class, TimelineElementCategoryV27.ANALOG_WORKFLOW_RECIPIENT_DECEASED.getValue(), TimelineElementCategoryInt.PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED, TimelineElementCategoryInt.VERSION_26),
    PUBLIC_REGISTRY_VALIDATION_CALL(PublicRegistryValidationCallDetailsInt.class, TimelineElementCategoryV27.PUBLIC_REGISTRY_VALIDATION_CALL.getValue(), TimelineElementCategoryInt.VERSION_27);

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
}
