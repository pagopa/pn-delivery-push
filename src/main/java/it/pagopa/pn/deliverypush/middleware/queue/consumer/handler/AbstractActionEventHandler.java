package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;

import java.util.function.Consumer;

@Slf4j
public abstract class AbstractActionEventHandler implements EventHandler<Action> {
    protected final TimelineUtils timelineUtils;

    public AbstractActionEventHandler(TimelineUtils timelineUtils) {
        this.timelineUtils = timelineUtils;
    }

    protected void checkNotificationCancelledAndExecute(Action action, Consumer<Action> functionToCall) {
        if (! timelineUtils.checkIsNotificationCancellationRequested(action.getIun())) {
            functionToCall.accept(action);
        } else {
            log.info("Notification is cancelled, the action will not be executed - iun={}", action.getIun());
        }
    }

    public Class<Action> getPayloadType() {
        return Action.class;
    }

    public abstract SupportedEventType getSupportedEventType();

    public abstract void handle(Action payload, MessageHeaders headers);

}
