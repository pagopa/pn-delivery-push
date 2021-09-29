package it.pagopa.pn.deliverypush.abstractions.actionspool;

public enum ActionType {
    SENDER_ACK() {

        @Override
        public String buildActionId(Action action) {
            return String.format("%s_start", action.getIun() );
        }
    },
    CHOOSE_DELIVERY_MODE {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_deliveryMode_rec%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },
    SEND_PEC() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_pec_rec%d_%s_n%d",
                    action.getIun(),
                    action.getRecipientIndex(),
                    action.getDigitalAddressSource(),
                    action.getRetryNumber()
                );
        }
    },
    END_OF_DIGITAL_DELIVERY_WORKFLOW() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_courtesy_rec%d",
                    action.getIun(),
                    action.getRecipientIndex()
                );
        }
    },
    RECEIVE_PEC() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_pec_result_rec%d_%s_n%d",
                    action.getIun(),
                    action.getRecipientIndex(),
                    action.getDigitalAddressSource(),
                    action.getRetryNumber()
            );
        }
    },
    WAIT_FOR_RECIPIENT_TIMEOUT() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_recipient_timeout_rec%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },
    NOTIFICATION_VIEWED() {
        @Override
        public String buildActionId(Action action) {
            return String.format("%s_notification_viewed%d", 
            					action.getIun(),
            					action.getRecipientIndex()
            );
        }
    };

    public String buildActionId(Action action) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
    }
}
