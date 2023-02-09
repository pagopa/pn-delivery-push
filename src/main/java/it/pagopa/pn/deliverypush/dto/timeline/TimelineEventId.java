package it.pagopa.pn.deliverypush.dto.timeline;

public enum TimelineEventId {
    SENDERACK_CREATION_REQUEST("senderack_legalfact_creation_request"),
    
    REQUEST_ACCEPTED("request_accepted"),

    //TODO capire meglio questo caso
    SEND_COURTESY_MESSAGE("send_courtesy_message") {
        private static final String EVENT_COMMON_PREFIX = "%s_send_courtesy_message_%d_type_";

        @Override
        public String buildEventId(EventId eventId) {
            String eventCommonId = EVENT_COMMON_PREFIX + "%s";
            return String.format(
                    eventCommonId,
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getCourtesyAddressType()==null?"":eventId.getCourtesyAddressType().getValue()   // se passo un courtesy null, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
            );
        }

        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex){
            return String.format(
                    EVENT_COMMON_PREFIX,
                    iun,
                    recipientIndex
            );
        }

    },
    
    GET_ADDRESS("get_address"),

    SEND_DIGITAL_FEEDBACK("send_digital_feedback"),

    SEND_DIGITAL_PROGRESS("digital_delivering_progress") {

        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex){
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(iun)
                    .withRecIndex(recipientIndex)
                    .build();
        }
    },
    
    SEND_ANALOG_FEEDBACK("send_analog_feedback"),

    SEND_ANALOG_PROGRESS("send_analog_progress"),

    SEND_DIGITAL_DOMICILE("send_digital_domicile"),

    PREPARE_SIMPLE_REGISTERED_LETTER("prepare_simple_registered_letter"),

    SEND_SIMPLE_REGISTERED_LETTER("send_simple_registered_letter"),

    PREPARE_ANALOG_DOMICILE("prepare_analog_domicile"),

    SEND_ANALOG_DOMICILE("send_analog_domicile"),

    DIGITAL_DELIVERY_CREATION_REQUEST("digital_delivery_creation_request"),
    
    DIGITAL_SUCCESS_WORKFLOW("digital_success_workflow"),

    DIGITAL_FAILURE_WORKFLOW("digital_failure_workflow"),

    ANALOG_SUCCESS_WORKFLOW("analog_success_workflow"),

    ANALOG_FAILURE_WORKFLOW("analog_failure_workflow"),

    NOTIFICATION_VIEWED_CREATION_REQUEST("notification_viewed_creation_request"),
    
    NOTIFICATION_VIEWED("notification_viewed"),

    COMPLETELY_UNREACHABLE("completely_unreachable"),

    REFINEMENT("refinement"),

    SCHEDULE_DIGITAL_WORKFLOW("schedule_digital_workflow"),

    SCHEDULE_ANALOG_WORKFLOW("schedule_analog_workflow"),

    SCHEDULE_REFINEMENT_WORKFLOW("schedule_refinement_workflow"),

    REQUEST_REFUSED("request_refused"),

    PUBLIC_REGISTRY_CALL("public_registry_call"),

    PUBLIC_REGISTRY_RESPONSE("public_registry_response"),

    AAR_CREATION_REQUEST("aar_creation_request"),
    
    AAR_GENERATION("aar_gen"),
    
    NOT_HANDLED("not_handled"),

    NOTIFICATION_PAID("notification_paid")
    ;

    public String buildEventId(EventId eventId) {
        return new TimelineEventIdBuilder().buildFromEventId(this, eventId);
    }

    public String buildEventId(String eventId) {
        return new TimelineEventIdBuilder().buildFromCorrelationId(this, eventId);
    }

    public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
    }

    private String value;

    TimelineEventId(String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
