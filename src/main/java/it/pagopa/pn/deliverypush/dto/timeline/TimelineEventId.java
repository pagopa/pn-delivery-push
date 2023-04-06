package it.pagopa.pn.deliverypush.dto.timeline;

public enum TimelineEventId {
    SENDERACK_CREATION_REQUEST("SENDERACK_LEGALFACT_CREATION_REQUEST") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },
    
    REQUEST_ACCEPTED("REQUEST_ACCEPTED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },

    SEND_COURTESY_MESSAGE("SEND_COURTESY_MESSAGE") {

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
    
    GET_ADDRESS("GET_ADDRESS") {
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

    SEND_DIGITAL_FEEDBACK("SEND_DIGITAL_FEEDBACK") {
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

    SEND_DIGITAL_PROGRESS("DIGITAL_DELIVERING_PROGRESS") {
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
    
    SEND_ANALOG_FEEDBACK("SEND_ANALOG_FEEDBACK") {
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

    SEND_ANALOG_PROGRESS("SEND_ANALOG_PROGRESS") {
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

    PREPARE_DIGITAL_DOMICILE("PREPARE_DIGITAL_DOMICILE") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .withCorrelationId(eventId.getRelatedTimelineId())
                    .build();
        }
    },

    SEND_DIGITAL_DOMICILE("SEND_DIGITAL_DOMICILE") {
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

    PREPARE_SIMPLE_REGISTERED_LETTER("PREPARE_SIMPLE_REGISTERED_LETTER") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    SEND_SIMPLE_REGISTERED_LETTER("SEND_SIMPLE_REGISTERED_LETTER") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    PREPARE_ANALOG_DOMICILE("PREPARE_ANALOG_DOMICILE") {
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

    SEND_ANALOG_DOMICILE("SEND_ANALOG_DOMICILE") {
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

    DIGITAL_DELIVERY_CREATION_REQUEST("DIGITAL_DELIVERY_CREATION_REQUEST") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },
    
    DIGITAL_SUCCESS_WORKFLOW("DIGITAL_SUCCESS_WORKFLOW") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    DIGITAL_FAILURE_WORKFLOW("DIGITAL_FAILURE_WORKFLOW") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    ANALOG_SUCCESS_WORKFLOW("ANALOG_SUCCESS_WORKFLOW") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    ANALOG_FAILURE_WORKFLOW("ANALOG_FAILURE_WORKFLOW") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    NOTIFICATION_VIEWED_CREATION_REQUEST("NOTIFICATION_VIEWED_CREATION_REQUEST") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },
    
    NOTIFICATION_VIEWED("NOTIFICATION_VIEWED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    COMPLETELY_UNREACHABLE("COMPLETELY_UNREACHABLE") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    REFINEMENT("REFINEMENT") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    SCHEDULE_DIGITAL_WORKFLOW("SCHEDULE_DIGITAL_WORKFLOW") {
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

    SCHEDULE_ANALOG_WORKFLOW("SCHEDULE_ANALOG_WORKFLOW") {
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

    SCHEDULE_REFINEMENT_WORKFLOW("SCHEDULE_REFINEMENT_WORKFLOW") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    REQUEST_REFUSED("REQUEST_REFUSED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },

    NATIONAL_REGISTRY_CALL("NATIONAL_REGISTRY_CALL") {
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

    NATIONAL_REGISTRY_RESPONSE("NATIONAL_REGISTRY_RESPONSE") {
        @Override
        public String buildEventId(String eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withCorrelationId(eventId)
                    .build();
        }
    },

    AAR_CREATION_REQUEST("AAR_CREATION_REQUEST") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },
    
    AAR_GENERATION("AAR_GEN") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },
    
    NOT_HANDLED("NOT_HANDLED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    NOTIFICATION_PAID("NOTIFICATION_PAID") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withPaymentCode(buildPaymentCode(eventId))
                    .build();
        }

        private String buildPaymentCode(EventId eventId) {
            String paymentCode;
            if(eventId.getIdF24() != null) {
                //per pagamenti f24
                paymentCode = "F24" + eventId.getIdF24();
            }
            else {
                //per pagamenti PagoPa
                paymentCode = "PPA" + eventId.getNoticeCode() + eventId.getCreditorTaxId();
            }
            return paymentCode;
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

    private final String value;

    TimelineEventId(String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
