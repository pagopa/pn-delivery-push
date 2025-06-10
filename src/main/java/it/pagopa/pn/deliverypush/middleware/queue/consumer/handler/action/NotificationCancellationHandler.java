package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
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
public class NotificationCancellationHandler extends AbstractActionEventHandler {
    private final NotificationCancellationActionHandler notificationCancellationActionHandler;

    public NotificationCancellationHandler(TimelineUtils timelineUtils, NotificationCancellationActionHandler notificationCancellationActionHandler) {
        super(timelineUtils);
        this.notificationCancellationActionHandler = notificationCancellationActionHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.NOTIFICATION_CANCELLATION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.NOTIFICATION_CANCELLATION.name();
        try {
            log.debug("Handle action of type NOTIFICATION_CANCELLATION, with payload {}", action);
            HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

            log.logStartingProcess(processName);
            notificationCancellationActionHandler.continueCancellationProcess(action.getIun());
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
