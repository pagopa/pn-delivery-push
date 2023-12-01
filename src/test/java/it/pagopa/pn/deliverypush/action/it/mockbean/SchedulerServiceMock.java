package it.pagopa.pn.deliverypush.action.it.mockbean;


import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class SchedulerServiceMock implements SchedulerService {
  private final ActionPoolMock actionPoolMock;

  public SchedulerServiceMock(ActionPoolMock actionPoolMock) {
    this.actionPoolMock = actionPoolMock;
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, ActionDetails actionDetails) {
    log.info("[TEST] Start scheduling - iun={} id={} actionType={} ", iun, recIndex, actionType);
    handleSchedulingAction(iun, recIndex, dateToSchedule, actionType, actionDetails, null);
  }

  private void handleSchedulingAction(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails, String timelineId) {
    Action action = Action.builder()
            .iun(iun)
            .recipientIndex(recIndex)
            .notBefore(dateToSchedule)
            .type(actionType)
            .details(actionDetails)
            .timelineId(timelineId)
            .build();
    actionPoolMock.addAction(action);
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, String timelineId) {
    log.info("[TEST] Start scheduling with timelineid - iun={} id={} actionType={} timelineid={} datetoschedule={}", iun, recIndex, actionType, timelineId, dateToSchedule);
    handleSchedulingAction(iun, recIndex, dateToSchedule, actionType, null, timelineId);
  }

  @Override
  public void unscheduleEvent(String iun, Integer recIndex, ActionType actionType,
      String timelineId) {
    // non usato come mock
  }

  @Override
  public void scheduleWebhookEvent(String paId, String iun, String timelineId) {
    // non usato come mock
  }

  @Override
  public void scheduleWebhookEvent(String streamId, String eventId, Integer delay,
      WebhookEventType actionType) {
    // non usato come mock
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType, String timelineId, ActionDetails actionDetails) {
      handleSchedulingAction(iun, recIndex, dateToSchedule, actionType, actionDetails, timelineId);
  }

  @Override
  public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails) {
    this.scheduleEvent(iun, null, dateToSchedule, actionType, actionDetails);
  }

  @Override
  public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType){
    this.scheduleEvent(iun, null, dateToSchedule, actionType);
  }

  @Override
  public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
      ActionType actionType) {
    this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, (ActionDetails) null);
  }
}