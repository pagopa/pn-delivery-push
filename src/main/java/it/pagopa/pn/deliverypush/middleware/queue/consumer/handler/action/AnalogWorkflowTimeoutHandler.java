package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowTimeoutActionHandler;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class AnalogWorkflowTimeoutHandler extends AbstractActionEventHandler {
    private final AnalogWorkflowTimeoutActionHandler analogWorkflowTimeoutActionHandler;

    public AnalogWorkflowTimeoutHandler(TimelineUtils timelineUtils, AnalogWorkflowTimeoutActionHandler analogWorkflowTimeoutActionHandler) {
        super(timelineUtils);
        this.analogWorkflowTimeoutActionHandler = analogWorkflowTimeoutActionHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT.name();

        try {
            log.debug("Handle action of type ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> analogWorkflowTimeoutActionHandler.handleAnalogWorkflowTimeout(a.getIun(), a.getTimelineId(), a.getRecipientIndex(), (AnalogWorkflowTimeoutDetails) a.getDetails(), a.getNotBefore())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
