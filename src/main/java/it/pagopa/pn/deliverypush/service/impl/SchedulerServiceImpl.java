package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhooksPool;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final ActionsPool actionsPool;
    private final WebhooksPool webhooksPool;

    public SchedulerServiceImpl(ActionsPool actionsPool, WebhooksPool webhooksPool) {
        this.actionsPool = actionsPool;
        this.webhooksPool = webhooksPool;
    }

    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null);
    }


    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, String timelineEventId) {
        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .notBefore(dateToSchedule)
                .type(actionType)
                .timelineId(timelineEventId)
                .build();

        this.actionsPool.scheduleFutureAction(action.toBuilder()
                .actionId(action.getType().buildActionId(action))
                .build()
        );
    }



    @Override
    public void unscheduleEvent(String iun, Integer recIndex, ActionType actionType, String timelineEventId) {
        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .type(actionType)
                .timelineId(timelineEventId)
                .build();

        this.actionsPool.unscheduleFutureAction (action.getType().buildActionId(action));
    }


    @Override
    public void scheduleWebhookEvent(String paId, String iun, String timelineId, Instant timestamp, String oldStatus, String newStatus, String timelineEventCategory) {
        WebhookAction action = WebhookAction.builder()
                .iun(iun)
                .paId(paId)
                .timestamp(timestamp)
                .eventId(timestamp + "_" + timelineId)
                //.delay(null)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .timelineEventCategory(timelineEventCategory)
                .type(WebhookEventType.REGISTER_EVENT)
                .build();

        this.webhooksPool.scheduleFutureAction(action);
    }


    @Override
    public void scheduleWebhookEvent(String streamId, String eventId, Integer delay, WebhookEventType actionType) {
        WebhookAction action = WebhookAction.builder()
                .streamId(streamId)
                .eventId(eventId)
                .iun("nd")
                .delay(delay)
                .type(actionType)
                .build();

        this.webhooksPool.scheduleFutureAction(action);
    }

}
