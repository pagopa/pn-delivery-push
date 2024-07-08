package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhooksPool;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final ActionsPool actionsPool;
    private final WebhooksPool webhooksPool;
    private final Clock clock;
    private final TimelineUtils timelineUtils;
    private final FeatureEnabledUtils featureEnabledUtils;

    @Override
    public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType) {
        this.scheduleEvent(iun, null, dateToSchedule, actionType, null, null, false);
    }
    
    @Override
    public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails){
        this.scheduleEvent(iun, null, dateToSchedule, actionType, null, actionDetails, false);
    }

    @Override
    public void scheduleEventNowOnlyIfAbsent(String iun, ActionType actionType, ActionDetails actionDetails){
        this.scheduleEvent(iun, null, Instant.now(), actionType, null, actionDetails, true);
    }

    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null, actionDetails, false);
    }
    
    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null, null, false);
    }

    @Override
    public void scheduleEvent(
            String iun,
            Integer recIndex,
            Instant dateToSchedule,
            ActionType actionType,
            String timelineEventId,
            ActionDetails actionDetails
    ) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, timelineEventId, actionDetails, false);
    }

    private void scheduleEvent(
            String iun, 
            Integer recIndex,
            Instant dateToSchedule,
            ActionType actionType,
            String timelineEventId,
            ActionDetails actionDetails,
            boolean scheduleNowIfAbsent
    ) {
        log.info("Schedule {} in schedulingDate={} - iun={}", actionType, dateToSchedule, iun);
        log.debug("ScheduleEvent iun={} recIndex={} dateToSchedule={} actionType={} timelineEventId={}", iun, recIndex, dateToSchedule, actionType, timelineEventId);
        
        if(! timelineUtils.checkIsNotificationCancellationRequested(iun)){
            Action action = Action.builder()
                    .iun(iun)
                    .recipientIndex(recIndex)
                    .notBefore(dateToSchedule)
                    .type(actionType)
                    .timelineId(timelineEventId)
                    .details(actionDetails)
                    .build();

            action = action.toBuilder()
                    .actionId(action.getType().buildActionId(action))
                    .build();
            
            if(featureEnabledUtils.isPerformanceImprovementEnabled(action.getNotBefore())) {
                actionsPool.addOnlyAction(action);
            }else {
                //Da eliminare Una volta stabilizzata la feature miglioramento performance workflow, che include una gestione diverse per le action. Qui andrà sempre e solo inserita una action
                if(! scheduleNowIfAbsent){
                    this.actionsPool.startActionOrScheduleFutureAction(action);
                } else {
                    this.actionsPool.scheduleFutureAction(action);
                }
            }

        } else {
            log.info("Notification is cancelled, the action {} will not be scheduled - iun={}", actionType, iun);
        }
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
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, timelineId, null, false);
    }
    
}
