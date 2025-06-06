package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
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
public class RefinementNotificationHandler extends AbstractActionEventHandler {
    private final RefinementHandler refinementHandler;

    public RefinementNotificationHandler(TimelineUtils timelineUtils, RefinementHandler refinementHandler) {
        super(timelineUtils);
        this.refinementHandler = refinementHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.REFINEMENT_NOTIFICATION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.REFINEMENT_NOTIFICATION.name();

        try {
            log.debug("Handle action of type REFINEMENT_NOTIFICATION, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> refinementHandler.handleRefinement(a.getIun(), a.getRecipientIndex())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
