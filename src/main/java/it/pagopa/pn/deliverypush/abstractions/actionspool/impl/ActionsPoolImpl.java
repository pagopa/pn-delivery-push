package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.LastPollForFutureActions;
import jnr.ffi.annotations.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
public class ActionsPoolImpl implements ActionsPool {

    private final MomProducer<ActionEvent> actionsQueue;
    private final ActionDao actionDao;
    private final Clock clock;
    private final LastPollForFutureActionsDao lastPollForFutureActionsDao;

    public ActionsPoolImpl(MomProducer<ActionEvent> actionsQueue, ActionDao actionDao, Clock clock, LastPollForFutureActionsDao lastPollForFutureActionsDao) {
        this.actionsQueue = actionsQueue;
        this.actionDao = actionDao;
        this.clock = clock;
        this.lastPollForFutureActionsDao = lastPollForFutureActionsDao;
    }

    @Override
    public void scheduleFutureAction(Action action) {
        final String timeSlot = computeTimeSlot( action.getNotBefore() );
        actionDao.addAction( action, timeSlot);
    }

    @Override
    public Optional<Action> loadActionById( String actionId) {
        return actionDao.getActionById( actionId );
    }

    private String computeTimeSlot(Instant notBefore) {
        OffsetDateTime nowUtc = notBefore.atOffset( ZoneOffset.UTC );
        int year = nowUtc.get( ChronoField.YEAR_OF_ERA );
        int month = nowUtc.get( ChronoField.MONTH_OF_YEAR );
        int day = nowUtc.get( ChronoField.DAY_OF_MONTH );
        int hour = nowUtc.get( ChronoField.HOUR_OF_DAY );
        int minute = nowUtc.get( ChronoField.MINUTE_OF_HOUR );
        return String.format("%04d-%02d-%02dT%02d:%02d", year, month, day, hour, minute);
    }




    @Scheduled( fixedDelay = 2 * 1000 )
    protected void pollForFutureActions() {
        // FIXME re-implement scheduling polling in a cluster-aware way.
        // Evaluate:
        // - TTLs + C.D.D.
        // - Separate microservice runned with a scheduled tast

        // FIXME: Keep track of "all scheduled until" and try to schedule from that date to now.

        Instant now = clock.instant();
        Optional<LastPollForFutureActions> lastPollForFutureActionsOptional = lastPollForFutureActionsDao.getLastPollForFutureActionsById();

        Instant lastPollExecuted;
        if(lastPollForFutureActionsOptional.isPresent()) {
            lastPollExecuted = lastPollForFutureActionsOptional.get().getLastPollExecuted();
        }else{
            //to add instant parameter from configuration, if parameter is null use code below
            lastPollExecuted = now.minus(2, ChronoUnit.HOURS);
        }

        Duration timeFromLastExcecution = Duration.between(lastPollExecuted,now);
        for (long i = timeFromLastExcecution.toMinutes()+1; i >= 0; i--) {
            Instant when = now.minus(i, ChronoUnit.MINUTES);
            String timeSlot = computeTimeSlot(when);
            log.debug("Check time slot {}", timeSlot);
            actionDao.findActionsByTimeSlot(timeSlot).stream()
                    .filter(action -> now.isAfter(action.getNotBefore()))
                    .forEach(action -> this.scheduleOne(action, timeSlot));
        }

        LastPollForFutureActions  lastPollForFutureActions =  LastPollForFutureActions.builder()
                .lastPollExecuted(now)
                .build();
        lastPollForFutureActionsDao.updateLastPollForFutureActions(lastPollForFutureActions);

    }

    private void scheduleOne( Action action, String timeSlot) {
        try {
            log.info("Scheduling action {}", action );
            addToActionsQueue( action );
            actionDao.unSchedule( action, timeSlot );
        }
        catch ( RuntimeException exc ) {
            log.error( "Scheduling action " + action, exc);
        }
    }

    private void addToActionsQueue( Action action ) {
        actionsQueue.push( ActionEvent.builder()
                .header( StandardEventHeader.builder()
                        .publisher("deliveryPush")
                        .iun( action.getIun() )
                        .eventId( action.getActionId() )
                        .createdAt( clock.instant() )
                        .eventType( ActionEventType.ACTION_GENERIC.name() )
                        .build()
                )
                .payload( action )
                .build()
        );
    }
}
