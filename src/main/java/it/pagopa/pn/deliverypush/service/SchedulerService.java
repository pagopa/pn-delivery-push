package it.pagopa.pn.deliverypush.service;

import java.time.Instant;
import java.util.Map;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;

public interface SchedulerService {
  void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType,
      Map<String, ?> notificationDetails);

  void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType,
      String timelineId, Map<String, ?> notificationDetails);

  void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType);

  void unscheduleEvent(String iun, Integer recIndex, ActionType actionType, String timelineId);

  void scheduleWebhookEvent(String paId, String iun, String timelineId);

  void scheduleWebhookEvent(String streamId, String eventId, Integer delay,
      WebhookEventType actionType);
}
