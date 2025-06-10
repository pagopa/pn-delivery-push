package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
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
public class ReceivedLegalFactGenerationHandler extends AbstractActionEventHandler {
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;

    public ReceivedLegalFactGenerationHandler(TimelineUtils timelineUtils, ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest) {
        super(timelineUtils);
        this.receivedLegalFactCreationRequest = receivedLegalFactCreationRequest;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.SCHEDULE_RECEIVED_LEGALFACT_GENERATION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.SCHEDULE_RECEIVED_LEGALFACT_GENERATION.name();

        try {
            log.debug("Handle action of type SCHEDULE_RECEIVED_LEGALFACT_GENERATION, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(a.getIun())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
