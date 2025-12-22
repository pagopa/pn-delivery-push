package it.pagopa.pn.deliverypush.dto.timeline;

public enum TimelineEventId {

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

    REQUEST_REFUSED("REQUEST_REFUSED") {
        @Override
        public String buildEventId(EventId eventId) {
            return new TimelineEventIdBuilder()
                    .withCategory(this.getValue())
                    .withIun(eventId.getIun())
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
    };

    public String buildEventId(EventId eventId) {
        throw new UnsupportedOperationException("Must be implemented for each action type event ID");
    }

    private final String value;

    TimelineEventId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
