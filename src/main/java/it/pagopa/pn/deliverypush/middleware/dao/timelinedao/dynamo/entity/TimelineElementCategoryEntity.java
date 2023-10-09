package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum TimelineElementCategoryEntity {
    SENDER_ACK_CREATION_REQUEST("SENDER_ACK_CREATION_REQUEST"),
    
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST("VALIDATE_NORMALIZE_ADDRESSES_REQUEST"),
    NORMALIZED_ADDRESS("NORMALIZED_ADDRESS"),

    VALIDATED_F24("VALIDATED_F24"),
    VALIDATE_F24_REQUEST("VALIDATE_F24_REQUEST"),
    
    REQUEST_ACCEPTED("REQUEST_ACCEPTED"),

    SEND_COURTESY_MESSAGE("SEND_COURTESY_MESSAGE"),

    GET_ADDRESS("GET_ADDRESS"),

    PUBLIC_REGISTRY_CALL("PUBLIC_REGISTRY_CALL"),

    PUBLIC_REGISTRY_RESPONSE("PUBLIC_REGISTRY_RESPONSE"),

    SCHEDULE_ANALOG_WORKFLOW("SCHEDULE_ANALOG_WORKFLOW"),

    SCHEDULE_DIGITAL_WORKFLOW("SCHEDULE_DIGITAL_WORKFLOW"),

    SEND_DIGITAL_DOMICILE("SEND_DIGITAL_DOMICILE"),

    PREPARE_DIGITAL_DOMICILE("PREPARE_DIGITAL_DOMICILE"),
    
    SEND_DIGITAL_PROGRESS("SEND_DIGITAL_PROGRESS"),

    SEND_DIGITAL_FEEDBACK("SEND_DIGITAL_FEEDBACK"),

    REFINEMENT("REFINEMENT"),

    SCHEDULE_REFINEMENT("SCHEDULE_REFINEMENT"),

    DIGITAL_DELIVERY_CREATION_REQUEST("DIGITAL_DELIVERY_CREATION_REQUEST"),
        
    DIGITAL_SUCCESS_WORKFLOW("DIGITAL_SUCCESS_WORKFLOW"),

    DIGITAL_FAILURE_WORKFLOW("DIGITAL_FAILURE_WORKFLOW"),

    ANALOG_SUCCESS_WORKFLOW("ANALOG_SUCCESS_WORKFLOW"),

    ANALOG_FAILURE_WORKFLOW("ANALOG_FAILURE_WORKFLOW"),

    COMPLETELY_UNREACHABLE_CREATION_REQUEST("COMPLETELY_UNREACHABLE_CREATION_REQUEST"),

    PREPARE_SIMPLE_REGISTERED_LETTER("PREPARE_SIMPLE_REGISTERED_LETTER"),

    SEND_SIMPLE_REGISTERED_LETTER("SEND_SIMPLE_REGISTERED_LETTER"),

    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS("SEND_SIMPLE_REGISTERED_LETTER_PROGRESS"),
    
    NOTIFICATION_VIEWED_CREATION_REQUEST("NOTIFICATION_VIEWED_CREATION_REQUEST"),

    NOTIFICATION_VIEWED("NOTIFICATION_VIEWED"),

    PREPARE_ANALOG_DOMICILE("PREPARE_ANALOG_DOMICILE"),

    PREPARE_ANALOG_DOMICILE_FAILURE("PREPARE_ANALOG_DOMICILE_FAILURE"),

    SEND_ANALOG_DOMICILE("SEND_ANALOG_DOMICILE"),

    SEND_ANALOG_PROGRESS("SEND_ANALOG_PROGRESS"),

    SEND_ANALOG_FEEDBACK("SEND_ANALOG_FEEDBACK"),

    PAYMENT("PAYMENT"),

    COMPLETELY_UNREACHABLE("COMPLETELY_UNREACHABLE"),

    REQUEST_REFUSED("REQUEST_REFUSED"),
    
    AAR_CREATION_REQUEST("AAR_CREATION_REQUEST"),
        
    AAR_GENERATION("AAR_GENERATION"),

    NOT_HANDLED("NOT_HANDLED"),

    PROBABLE_SCHEDULING_ANALOG_DATE("PROBABLE_SCHEDULING_ANALOG_DATE"),

    NOTIFICATION_CANCELLATION_REQUEST("NOTIFICATION_CANCELLATION_REQUEST"),

    NOTIFICATION_CANCELLED("NOTIFICATION_CANCELLED");
    
    private final String value;

    TimelineElementCategoryEntity(String value) {
        this.value = value;
    }

}
