package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.startworkflow.ScheduleRecipientWorkflow;
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
public class PostAcceptedProcessingCompletedHandler extends AbstractActionEventHandler {
    private final ScheduleRecipientWorkflow scheduleRecipientWorkflow;

    public PostAcceptedProcessingCompletedHandler(TimelineUtils timelineUtils, ScheduleRecipientWorkflow scheduleRecipientWorkflow) {
        super(timelineUtils);
        this.scheduleRecipientWorkflow = scheduleRecipientWorkflow;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.POST_ACCEPTED_PROCESSING_COMPLETED;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.POST_ACCEPTED_PROCESSING_COMPLETED.name();

        try {
            log.debug("Handle action of type POST_ACCEPTED_PROCESSING_COMPLETED, with payload {}", action);
            log.logStartingProcess(processName);

            scheduleRecipientWorkflow.startScheduleRecipientWorkflow(action.getIun());
            log.logEndingProcess(processName);
        }catch (Exception ex){
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
