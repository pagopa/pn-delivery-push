package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;

import java.time.Instant;

public interface SchedulerService {
    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType);

    void scheduleWebhookEvent(String iun, Integer recIndex, Instant dateToSchedule, WebhookEventType actionType);

    void scheduleWebhookEvent(String streamId, String eventId, String);

}
