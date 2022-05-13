package it.pagopa.pn.deliverypush.abstractions.actionspool;

public enum ActionType {
    ANALOG_WORKFLOW() { //NEW
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_analog_workflow_e_%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },
    
    DIGITAL_WORKFLOW_NEXT_ACTION() { //NEW
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_digital_workflow_e_%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },

    REFINEMENT_NOTIFICATION() { //NEW
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_refinement_notification_%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    };
    /*
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
    SEND_PAPER() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_paper_rec%d_n%d",
                    action.getIun(),
                    action.getRecipientIndex(),
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
    END_OF_ANALOG_DELIVERY_WORKFLOW() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_end_analog_rec%d",
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
    RECEIVE_PAPER() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_paper_result_rec%d_n%d",
                    action.getIun(),
                    action.getRecipientIndex(),
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
            return String.format("%s_notification_viewed_rec%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },

    PEC_FAIL_SEND_PAPER() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_paper_after_digital_rec%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },

    PEC_FAIL_RECEIVE_PAPER() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_receive_paper_after_digital_rec%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },

    COMPLETELY_UNREACHABLE() {
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_completely_unreachable_%d",
                    action.getIun(),
                    action.getRecipientIndex()
            );
        }
    },
    
     */



    public String buildActionId(Action action) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
    }
}
