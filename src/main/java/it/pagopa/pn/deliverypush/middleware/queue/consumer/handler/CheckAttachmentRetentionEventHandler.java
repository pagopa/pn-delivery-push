package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.checkattachmentretention.CheckAttachmentRetentionHandler;
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
public class CheckAttachmentRetentionEventHandler extends AbstractActionEventHandler {
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;

    public CheckAttachmentRetentionEventHandler(TimelineUtils timelineUtils, CheckAttachmentRetentionHandler checkAttachmentRetentionHandler) {
        super(timelineUtils);
        this.checkAttachmentRetentionHandler = checkAttachmentRetentionHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.CHECK_ATTACHMENT_RETENTION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.CHECK_ATTACHMENT_RETENTION.name();

        try {
            log.debug("Handle action of type CHECK_ATTACHMENT_RETENTION, with payload {}", action);
            HandleEventUtils.addIunAndCorrIdToMdc(action.getIun(), action.getActionId());

            log.logStartingProcess(processName);

            checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(action.getIun(), action.getNotBefore());
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
