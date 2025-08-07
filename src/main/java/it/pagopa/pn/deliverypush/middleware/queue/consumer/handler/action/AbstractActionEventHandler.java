package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public abstract class AbstractActionEventHandler implements EventHandler<Action> {
    protected final TimelineUtils timelineUtils;

    protected AbstractActionEventHandler(TimelineUtils timelineUtils) {
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
}
