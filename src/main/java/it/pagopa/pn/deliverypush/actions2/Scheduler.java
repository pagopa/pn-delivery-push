package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;

import java.time.Instant;

public interface Scheduler {
    public void schedulEvent(Instant dateToSchedule, ActionType actionType);
}
