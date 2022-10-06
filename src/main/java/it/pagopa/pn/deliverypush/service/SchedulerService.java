package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;

import java.time.Instant;

public interface SchedulerService {
    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType);

    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, String timelineId);

    void unscheduleEvent(String iun, Integer recIndex, ActionType actionType, String timelineId);

    void scheduleWebhookEvent(String paId, String iun, String timelineId, Instant timestamp, String oldStatus, String newStatus, String timelineEventCategory);

    void scheduleWebhookEvent(String streamId, String eventId, Integer delay, WebhookEventType actionType);
}
