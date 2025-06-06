package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
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
public class DigitalWorkflowNextExecuteActionHandler extends AbstractActionEventHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;

    public DigitalWorkflowNextExecuteActionHandler(TimelineUtils timelineUtils, DigitalWorkFlowHandler digitalWorkFlowHandler) {
        super(timelineUtils);
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION.name();

        try {
            log.debug("Handle action of type DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> digitalWorkFlowHandler.startNextWorkFlowActionExecute(a.getIun(), a.getRecipientIndex(), a.getTimelineId())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
