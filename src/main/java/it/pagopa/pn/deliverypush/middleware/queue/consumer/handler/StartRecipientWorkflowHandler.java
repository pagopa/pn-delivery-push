package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class StartRecipientWorkflowHandler extends AbstractActionEventHandler {
    private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;

    public StartRecipientWorkflowHandler(TimelineUtils timelineUtils, StartWorkflowForRecipientHandler startWorkflowForRecipientHandler) {
        super(timelineUtils);
        this.startWorkflowForRecipientHandler = startWorkflowForRecipientHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.START_RECIPIENT_WORKFLOW;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.START_RECIPIENT_WORKFLOW.name();

        try {
            log.debug("Handle action of type START_RECIPIENT_WORKFLOW, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
            log.logStartingProcess(processName);

            checkNotificationCancelledAndExecute(
                    action,
                    a -> startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(a.getIun(), a.getRecipientIndex(), (RecipientsWorkflowDetails) a.getDetails())
            );

            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
