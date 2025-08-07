package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogFinalStatusResponseHandler;
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
public class SendAnalogFinalStatusResponseHandler extends AbstractActionEventHandler {
    private final AnalogFinalStatusResponseHandler analogFinalResponseHandler;

    public SendAnalogFinalStatusResponseHandler(TimelineUtils timelineUtils, AnalogFinalStatusResponseHandler analogFinalResponseHandler) {
        super(timelineUtils);
        this.analogFinalResponseHandler = analogFinalResponseHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.SEND_ANALOG_FINAL_STATUS_RESPONSE;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.SEND_ANALOG_FINAL_STATUS_RESPONSE.name();

        try {
            log.debug("Handle action of type SEND_ANALOG_FINAL_STATUS_RESPONSE, with payload {}", action);
            HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

            log.logStartingProcess(processName);
            analogFinalResponseHandler.handleFinalResponse(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
