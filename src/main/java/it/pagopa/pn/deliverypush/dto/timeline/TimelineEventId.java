package it.pagopa.pn.deliverypush.dto.timeline;

public enum TimelineEventId {
    SENDERACK_CREATION_REQUEST("senderack_legalfact_creation_request") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },
    
    REQUEST_ACCEPTED("request_accepted") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },

    SEND_COURTESY_MESSAGE("send_courtesy_message") {

        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withCourtesyAddressType(eventId.getCourtesyAddressType())
                    .build();
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
        }
    },

    REQUEST_REFUSED("request_refused") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
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
        }
    },

    PUBLIC_REGISTRY_RESPONSE("public_registry_response") {
        @Override
        public String buildEventId(String eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withCorrelationId(eventId)
                    .build();
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
        }
    },

    NOTIFICATION_PAID("notification_paid") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
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
