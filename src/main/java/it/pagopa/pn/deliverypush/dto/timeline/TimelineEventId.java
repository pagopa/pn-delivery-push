package it.pagopa.pn.deliverypush.dto.timeline;

public enum TimelineEventId {
    SENDERACK_CREATION_REQUEST("senderack_legalfact_creation_request") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
//            return String.format(
//                    "senderack_legalfact_creation_request_iun_%s",
//                    eventId.getIun()
//            );
        }
    },
    
    REQUEST_ACCEPTED("request_accepted") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
//            return String.format(
//                    "%s_request_accepted",
//                    eventId.getIun()
//            );
        }
    },

    SEND_COURTESY_MESSAGE("send_courtesy_message") {
//        private static final String EVENT_COMMON_PREFIX = "%s_send_courtesy_message_%d_type_";

        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withCourtesyAddressType(eventId.getCourtesyAddressType())
                    .build();
//            String eventCommonId = EVENT_COMMON_PREFIX + "%s";
//            return String.format(
//                    eventCommonId,
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getCourtesyAddressType()==null?"":eventId.getCourtesyAddressType().getValue()   // se passo un courtesy null, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
//            );
        }

        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex){
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(iun)
                    .withRecIndex(recipientIndex)
                    .build();
        }

    },
    
    GET_ADDRESS("get_address") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            return String.format(
//                    "%s_get_address_%d_source_%s_attempt_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSource(),
//                    eventId.getSentAttemptMade()
//            );
        }
    },

    SEND_DIGITAL_FEEDBACK("send_digital_feedback") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            return String.format(
//                    "%s_digital_delivering_progress_%d_source_%s_attempt_%d_progidx_%s",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSource(),
//                    eventId.getSentAttemptMade(),
//                    eventId.getProgressIndex()<0?"":eventId.getProgressIndex()  // se passo un progressindex negativo, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
//            );
        }
    },

    SEND_DIGITAL_PROGRESS("digital_delivering_progress") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .withProgressIndex(eventId.getProgressIndex())
                    .build();
//            return String.format(
//                    "%s_digital_delivering_progress_%d_source_%s_attempt_%d_progidx_%s",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSource(),
//                    eventId.getSentAttemptMade(),
//                    eventId.getProgressIndex()<0?"":eventId.getProgressIndex()  // se passo un progressindex negativo, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
//            );
        }
        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex){
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(iun)
                    .withRecIndex(recipientIndex)
                    .build();
        }
    },
    
    SEND_ANALOG_FEEDBACK("send_analog_feedback") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            return String.format(
//                    "%s_send_analog_feedback_%d_attempt_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSentAttemptMade()
//            );
        }
    },

    SEND_ANALOG_PROGRESS("send_analog_progress") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .withProgressIndex(eventId.getProgressIndex())
                    .build();
//            return String.format(
//                    "%s_send_analog_progress_%d_attempt_%d_progidx_%s",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSentAttemptMade(),
//                    eventId.getProgressIndex()<0?"":eventId.getProgressIndex()  // se passo un progressindex negativo, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
//            );
        }
    },

    SEND_DIGITAL_DOMICILE("send_digital_domicile") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            int sendAttempt = eventId.getSentAttemptMade();
//            return String.format(
//                    "%s_send_digital_domicile_%d_source_%s_attempt_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSource(),
//                    sendAttempt
//            );
        }
    },

    PREPARE_SIMPLE_REGISTERED_LETTER("prepare_simple_registered_letter") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_prepare_simple_registered_letter_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    SEND_SIMPLE_REGISTERED_LETTER("send_simple_registered_letter") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_send_simple_registered_letter_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    PREPARE_ANALOG_DOMICILE("prepare_analog_domicile") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            int sendAttempt = eventId.getSentAttemptMade();
//            return String.format(
//                    "%s_prepare_analog_domicile_%d_attempt_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    sendAttempt
//            );
        }
    },

    SEND_ANALOG_DOMICILE("send_analog_domicile") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            int sendAttempt = eventId.getSentAttemptMade();
//            return String.format(
//                    "%s_send_analog_domicile_%d_attempt_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    sendAttempt
//            );
        }
    },

    DIGITAL_DELIVERY_CREATION_REQUEST("digital_delivery_creation_request") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "digital_delivery_creation_request_iun_%s_recindex_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },
    
    DIGITAL_SUCCESS_WORKFLOW("digital_success_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_digital_success_workflow_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    DIGITAL_FAILURE_WORKFLOW("digital_failure_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_digital_failure_workflow_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    ANALOG_SUCCESS_WORKFLOW("analog_success_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_analog_success_workflow_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    ANALOG_FAILURE_WORKFLOW("analog_failure_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_analog_failure_workflow_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    NOTIFICATION_VIEWED_CREATION_REQUEST("notification_viewed_creation_request") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "notification_viewed_creation_request_iun_%s_recIndex_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },
    
    NOTIFICATION_VIEWED("notification_viewed") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_notification_viewed_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    COMPLETELY_UNREACHABLE("completely_unreachable") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_completely_unreachable_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    REFINEMENT("refinement") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_refinement_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    SCHEDULE_DIGITAL_WORKFLOW("schedule_digital_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            return String.format(
//                    "%s_schedule_digital_workflow_%d_source_%s_retry_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSource().getValue(),
//                    eventId.getSentAttemptMade()
//            );
        }
    },

    SCHEDULE_ANALOG_WORKFLOW("schedule_analog_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            return String.format(
//                    "%s_schedule_analog_workflow_%d_retry_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getSentAttemptMade()
//            );
        }
    },

    SCHEDULE_REFINEMENT_WORKFLOW("schedule_refinement_workflow") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_schedule_refinement_workflow_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    REQUEST_REFUSED("request_refused") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
//            return String.format(
//                    "%s_request_refused",
//                    eventId.getIun()
//            );
        }
    },

    PUBLIC_REGISTRY_CALL("public_registry_call") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withDeliveryMode(eventId.getDeliveryMode())
                    .withContactPhase(eventId.getContactPhase())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .build();
//            return String.format(
//                    "%s_%d_%s_%s_%d_%s",
//                    eventId.getIun(),
//                    eventId.getRecIndex(),
//                    eventId.getDeliveryMode(),
//                    eventId.getContactPhase(),
//                    eventId.getSentAttemptMade(),
//                    "public_registry_call"
//            );
        }
    },

    PUBLIC_REGISTRY_RESPONSE("public_registry_response") {
        @Override
        public String buildEventId(String eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withCorrelationId(eventId)
                    .build();
//            return String.format(
//                    "public_registry_response_%s",
//                    eventId
//            );
        }
    },

    AAR_CREATION_REQUEST("aar_creation_request") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "aar_creation_request_iun_%s_recIndex_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },
    
    AAR_GENERATION("aar_gen") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_aar_gen_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },
    
    NOT_HANDLED("not_handled") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
//            return String.format(
//                    "%s_not_handled_%d",
//                    eventId.getIun(),
//                    eventId.getRecIndex()
//            );
        }
    },

    NOTIFICATION_PAID("notification_paid") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
//            return String.format(
//                    "%s_notification_paid",
//                    eventId.getIun()
//            );
        }
    }
    ;

    public String buildEventId(EventId eventId) {
        throw new UnsupportedOperationException("Must be implemented for each action type event ID");
    }

    public String buildEventId(String eventId) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
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
