package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@CustomLog
public class ActionsPoolImpl implements ActionsPool {
    private final ActionService actionService;
    
    public ActionsPoolImpl( ActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public void addOnlyAction(Action action){
        final String timeSlot = computeTimeSlot( action.getNotBefore() );
        action = action.toBuilder()
                .timeslot( timeSlot)
                .build();
        actionService.addOnlyActionIfAbsent(action);
    }

    private String computeTimeSlot(Instant instant) {
        OffsetDateTime nowUtc = instant.atOffset(ZoneOffset.UTC);
        return String.format("%04d-%02d-%02dT%02d:%02d",
                nowUtc.getYear(),
                nowUtc.getMonthValue(),
                nowUtc.getDayOfMonth(),
                nowUtc.getHour(),
                nowUtc.getMinute());
    }

    @Override
    public void unscheduleFutureAction(String actionId) {
        actionService.unSchedule(actionId);
    }
}
