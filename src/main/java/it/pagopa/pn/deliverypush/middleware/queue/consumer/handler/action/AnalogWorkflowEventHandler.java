package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
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
public class AnalogWorkflowEventHandler extends AbstractActionEventHandler {
    private final AnalogWorkflowHandler analogWorkflowHandler;

    public AnalogWorkflowEventHandler(TimelineUtils timelineUtils, AnalogWorkflowHandler analogWorkflowHandler) {
        super(timelineUtils);
        this.analogWorkflowHandler = analogWorkflowHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.ANALOG_WORKFLOW;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.ANALOG_WORKFLOW.name();

        try {
            log.debug("Handle action of type ANALOG_WORKFLOW, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> analogWorkflowHandler.startAnalogWorkflow(a.getIun(), a.getRecipientIndex())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
