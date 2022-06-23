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
    },
    SENDER_ACK() {

        @Override
        public String buildActionId(Action action) {
            return String.format("%s_start", action.getIun() );
        }
    };

    public String buildActionId(Action action) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
    }
}
