package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
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
public class DigitalWorkflowRetryActionHandler extends AbstractActionEventHandler {
    private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;

    public DigitalWorkflowRetryActionHandler(TimelineUtils timelineUtils, DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler) {
        super(timelineUtils);
        this.digitalWorkFlowRetryHandler = digitalWorkFlowRetryHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.DIGITAL_WORKFLOW_RETRY_ACTION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.DIGITAL_WORKFLOW_RETRY_ACTION.name();

        try {
            log.debug("Handle action of type DIGITAL_WORKFLOW_RETRY_ACTION, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(a.getIun(), a.getRecipientIndex(), a.getTimelineId())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
