package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
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
public class ChooseDeliveryModeEventHandler extends AbstractActionEventHandler {
    private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;

    public ChooseDeliveryModeEventHandler(TimelineUtils timelineUtils, ChooseDeliveryModeHandler chooseDeliveryModeHandler) {
        super(timelineUtils);
        this.chooseDeliveryModeHandler = chooseDeliveryModeHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.CHOOSE_DELIVERY_MODE;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.CHOOSE_DELIVERY_MODE.name();

        try {
            log.debug("Handle action of type CHOOSE_DELIVERY_MODE, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(a.getIun(), a.getRecipientIndex())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
