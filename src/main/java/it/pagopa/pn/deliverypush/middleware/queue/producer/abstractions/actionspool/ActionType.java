package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import it.pagopa.pn.deliverypush.action.details.*;
import lombok.Getter;

@Getter
public enum ActionType {

  NOTIFICATION_VALIDATION(NotificationValidationActionDetails.class) {
    @Override
    public String buildActionId(Action action) {
      NotificationValidationActionDetails details = (NotificationValidationActionDetails) action.getDetails();
      
      return String.format("notification_validation_iun_%s_retry=%d", 
              action.getIun(),
              details.getRetryAttempt());
    }
  },

  NOTIFICATION_REFUSED(NotificationRefusedActionDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("notification_refused_iun_%s",
              action.getIun()
      );
    }
  },
  
  NOTIFICATION_CANCELLATION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("notification_cancellation_iun_%s", action.getIun());
    }
  },

  SCHEDULE_RECEIVED_LEGALFACT_GENERATION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("schedule_creation_received_iun_%s",
              action.getIun()
      );
    }
  },

  CHECK_ATTACHMENT_RETENTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("check_attachment_retention_iun_%s_scheduling-date_%s",
              action.getIun(),
              action.getNotBefore()
      );
    }
  },
  
  START_RECIPIENT_WORKFLOW(RecipientsWorkflowDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_start_recipient_workflow_%d",
              action.getIun(), 
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
      return String.format("%s_digital_workflow_e_%d_timelineid_%s", action.getIun(), action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },


  DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_execute_e_%d_timelineid_%s", action.getIun(), action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
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
  },

  DOCUMENT_CREATION_RESPONSE(DocumentCreationResponseActionDetails.class) {
    @Override
    public String buildActionId(Action action) {
        return String.format("safe_storage_response_timelineId=%s",
                action.getTimelineId()
        );
    }
    
  };

  private final Class<? extends ActionDetails> detailsJavaClass;

  ActionType(Class<? extends ActionDetails> detailsJavaClass) {
    this.detailsJavaClass = detailsJavaClass;
  }

  public String buildActionId(Action action) {
    throw new UnsupportedOperationException("Must be implemented for each action type");
  }

}
