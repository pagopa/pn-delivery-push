package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;

import java.time.Instant;

public interface SchedulerService {
    void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType);
}
