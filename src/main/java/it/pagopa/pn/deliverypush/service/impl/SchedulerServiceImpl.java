package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhooksPool;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final ActionsPool actionsPool;
    private final WebhooksPool webhooksPool;
    private final Clock clock;

    public SchedulerServiceImpl(ActionsPool actionsPool, WebhooksPool webhooksPool, Clock clock) {
        this.actionsPool = actionsPool;
        this.webhooksPool = webhooksPool;
        this.clock = clock;
    }
    
    @Override
    public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails){
        this.scheduleEvent(iun, null, dateToSchedule, actionType, null, actionDetails);
    }
    
    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null, actionDetails);
    }
    
    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null, null);
    }
    
    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, String timelineEventId, ActionDetails actionDetails) {
        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .notBefore(dateToSchedule)
                .type(actionType)
                .timelineId(timelineEventId)
                .details(actionDetails)
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
    public void scheduleWebhookEvent(String paId, String iun, String timelineId) {
        WebhookAction action = WebhookAction.builder()
                .iun(iun)
                .paId(paId)
                .timelineId(timelineId)
                .eventId(clock.instant() + "_" + timelineId)
                //.delay(null)
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

    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
        ActionType actionType, String timelineId) {
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, timelineId, null);
    }

}
