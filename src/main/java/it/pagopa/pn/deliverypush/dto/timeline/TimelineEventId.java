package it.pagopa.pn.deliverypush.dto.timeline;

public enum TimelineEventId {

    REQUEST_ACCEPTED() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_request_accepted",
                    eventId.getIun()
            );
        }
    },
    
    SEND_COURTESY_MESSAGE() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_send_courtesy_message_%d_index_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getIndex()
            );
        }
    },
    
    GET_ADDRESS() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_get_address_%d_source_%s_attempt_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSource(),
                    eventId.getSentAttemptMade()
            );
        }
    },

    SEND_DIGITAL_FEEDBACK() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_send_digital_feedback_%d_source_%s_attempt_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSource(),
                    eventId.getSentAttemptMade()
            );
        }
    },

    SEND_DIGITAL_PROGRESS() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_digital_delivering_progress_%d_source_%s_attempt_%d_progidx_%s",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSource(),
                    eventId.getSentAttemptMade(),
                    eventId.getProgressIndex()<0?"":eventId.getProgressIndex()  // se passo un progressindex negativo, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
                    );
        }

        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex){
            return String.format(
                    "%s_digital_delivering_progress_%d_",
                    iun,
                    recipientIndex
            );
        }
    },
    
    SEND_ANALOG_FEEDBACK() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_send_analog_feedback_%d_attempt_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSentAttemptMade()
            );
        }
    },

    SEND_ANALOG_PROGRESS() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_send_analog_progress_%d_attempt_%d_progidx_%s",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSentAttemptMade(),
                    eventId.getProgressIndex()<0?"":eventId.getProgressIndex()  // se passo un progressindex negativo, è perchè non voglio che venga inserito nell'eventid. Usato per cercare con l'inizia per
            );
        }
    },

    SEND_DIGITAL_DOMICILE() {
        @Override
        public String buildEventId(EventId eventId) {
            int sendAttempt = eventId.getSentAttemptMade();
            return String.format(
                    "%s_send_digital_domicile_%d_source_%s_attempt_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSource(),
                    sendAttempt
            );
        }
    },

    PREPARE_SIMPLE_REGISTERED_LETTER() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_prepare_simple_registered_letter_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    SEND_SIMPLE_REGISTERED_LETTER() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_send_simple_registered_letter_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    PREPARE_ANALOG_DOMICILE() {
        @Override
        public String buildEventId(EventId eventId) {
            int sendAttempt = eventId.getSentAttemptMade();
            return String.format(
                    "%s_prepare_analog_domicile_%d_attempt_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    sendAttempt
            );
        }
    },

    SEND_ANALOG_DOMICILE() {
        @Override
        public String buildEventId(EventId eventId) {
            int sendAttempt = eventId.getSentAttemptMade();
            return String.format(
                    "%s_send_analog_domicile_%d_attempt_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    sendAttempt
            );
        }
    },

    DIGITAL_SUCCESS_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_digital_success_workflow_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    DIGITAL_FAILURE_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_digital_failure_workflow_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    ANALOG_SUCCESS_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_analog_success_workflow_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    ANALOG_FAILURE_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_analog_failure_workflow_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    NOTIFICATION_VIEWED() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_notification_viewed_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    COMPLETELY_UNREACHABLE() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_completely_unreachable_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    REFINEMENT() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_refinement_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    SCHEDULE_DIGITAL_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_schedule_digital_workflow_%d_source_%s_retry_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSource().getValue(),
                    eventId.getSentAttemptMade()
            );
        }
    },

    SCHEDULE_ANALOG_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_schedule_analog_workflow_%d_retry_%d",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getSentAttemptMade()
            );
        }
    },

    SCHEDULE_REFINEMENT_WORKFLOW() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_schedule_refinement_workflow_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    REQUEST_REFUSED() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_request_refused",
                    eventId.getIun()
            );
        }
    },

    PUBLIC_REGISTRY_CALL() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_%d_%s_%s_%d_%s",
                    eventId.getIun(),
                    eventId.getRecIndex(),
                    eventId.getDeliveryMode(),
                    eventId.getContactPhase(),
                    eventId.getSentAttemptMade(),
                    "public_registry_call"
            );
        }
    },

    PUBLIC_REGISTRY_RESPONSE() {
        @Override
        public String buildEventId(String eventId) {
            return String.format(
                    "public_registry_response_%s",
                    eventId
            );
        }
    },
    
    AAR_GENERATION() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_aar_gen_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },
    
    NOT_HANDLED() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_not_handled_%d",
                    eventId.getIun(),
                    eventId.getRecIndex()
            );
        }
    },

    NOTIFICATION_PAID() {
        @Override
        public String buildEventId(EventId eventId) {
            return String.format(
                    "%s_notification_paid",
                    eventId.getIun()
            );
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
}
