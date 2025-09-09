package it.pagopa.pn.deliverypush.dto.timeline;

import lombok.Getter;

@Getter
public enum TimelineEventId {
    SEND_COURTESY_MESSAGE("SEND_COURTESY_MESSAGE") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withCourtesyAddressType(eventId.getCourtesyAddressType())
                    .withOptin(eventId.getOptin())
                    .build();
        }

        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(iun)
                    .withRecIndex(recipientIndex)
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


    SEND_DIGITAL_PROGRESS("DIGITAL_PROG") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .withSource(eventId.getSource())
                    .withIsFirstSendRetry(eventId.getIsFirstSendRetry())
                    .withSentAttemptMade(eventId.getSentAttemptMade())
                    .withProgressIndex(eventId.getProgressIndex())
                    .build();
        }

        @Override
        public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(iun)
                    .withRecIndex(recipientIndex)
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

    ANALOG_WORKFLOW_RECIPIENT_DECEASED("ANALOG_WORKFLOW_RECIPIENT_DECEASED") {
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
            //per pagamenti PagoPa
            paymentCode = "PPA";
            if (eventId.getNoticeCode() != null) {
                paymentCode += eventId.getNoticeCode();
            }
            if (eventId.getCreditorTaxId() != null) {
                paymentCode += eventId.getCreditorTaxId();
            }
            return paymentCode;
        }
    },

    NOTIFICATION_CANCELLATION_REQUEST("NOTIFICATION_CANCELLATION_REQUEST") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }

    },

    NOTIFICATION_CANCELLED("NOTIFICATION_CANCELLED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },
    NOTIFICATION_RADD_RETRIEVED("NOTIFICATION_RADD_RETRIEVED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    },

    NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST("NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .build();
        }
    },

    NATIONAL_REGISTRY_VALIDATION_CALL("NATIONAL_REGISTRY_VALIDATION_CALL") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
                    .withDeliveryMode(eventId.getDeliveryMode())
                    .build();
        }
    },

    NATIONAL_REGISTRY_VALIDATION_RESPONSE("NATIONAL_REGISTRY_VALIDATION_RESPONSE") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withCorrelationId(eventId.getRelatedTimelineId())
                    .withRecIndex(eventId.getRecIndex())
                    .build();
        }
    };

    public String buildEventId(EventId eventId) {
        throw new UnsupportedOperationException("Must be implemented for each action type event ID");
    }

    public String buildSearchEventIdByIunAndRecipientIndex(String iun, Integer recipientIndex) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
    }

    private final String value;

    TimelineEventId(String value) {
        this.value = value;
    }

}
