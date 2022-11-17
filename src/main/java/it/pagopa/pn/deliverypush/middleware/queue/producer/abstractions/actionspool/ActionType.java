package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import it.pagopa.pn.deliverypush.action.details.NotHandledDetails;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import lombok.Getter;

@Getter
public enum ActionType {

  START_RECIPIENT_WORKFLOW(RecipientsWorkflowDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_start_recipient_workflow_%d", action.getIun(),
          action.getRecipientIndex());
    }
  },

  CHOOSE_DELIVERY_MODE(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_choose_delivery_mode_%d", action.getIun(),
          action.getRecipientIndex());
    }
  },

  ANALOG_WORKFLOW(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_analog_workflow_e_%d", action.getIun(), action.getRecipientIndex());
    }
  },

  DIGITAL_WORKFLOW_NEXT_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_e_%d", action.getIun(), action.getRecipientIndex());
    }
  },


  DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_no_response_timeount_e_%d_%s", action.getIun(),
          action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },

  DIGITAL_WORKFLOW_RETRY_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_retry_e_%d_%s", action.getIun(),
          action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },

  REFINEMENT_NOTIFICATION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_refinement_notification_%d", action.getIun(),
          action.getRecipientIndex());
    }
  },
  SENDER_ACK(NotHandledDetails.class) {

    @Override
    public String buildActionId(Action action) {
      return String.format("%s_start", action.getIun());
    }
  };

  private final Class<? extends ActionDetails> detailsJavaClass;

  private ActionType(Class<? extends ActionDetails> detailsJavaClass) {
    this.detailsJavaClass = detailsJavaClass;
  }

  public String buildActionId(Action action) {
    throw new UnsupportedOperationException("Must be implemented for each action type");
  }

}
