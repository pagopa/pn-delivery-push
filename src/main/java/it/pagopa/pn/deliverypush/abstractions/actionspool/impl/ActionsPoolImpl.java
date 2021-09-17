package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ActionsPoolImpl implements ActionsPool {

    private final MomProducer<ActionEvent> actionsQueue;
    private final ActionDao actionDao;
    private final Clock clock;
    private final LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao;
    private final PnDeliveryPushConfigs configs;

    public ActionsPoolImpl(MomProducer<ActionEvent> actionsQueue, ActionDao actionDao,
                           Clock clock, LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao, PnDeliveryPushConfigs configs) {
        this.actionsQueue = actionsQueue;
        this.actionDao = actionDao;
        this.clock = clock;
        this.lastFutureActionPoolExecutionTimeDao = lastFutureActionPoolExecutionTimeDao;
        this.configs = configs;
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

    private List<String> computeTimeSlots(Instant from, Instant to) {
        List<String> timeSlots = new ArrayList<>();
        Instant timeSlotStart = from.truncatedTo( ChronoUnit.MINUTES );
        while( timeSlotStart.isBefore( to )) {
            String timeSlot = computeTimeSlot( timeSlotStart );
            timeSlots.add( timeSlot );

            timeSlotStart = timeSlotStart.plus( 1, ChronoUnit.MINUTES );
        }
        return timeSlots;
    }

    private String computeTimeSlot(Instant instant) {
        OffsetDateTime nowUtc = instant.atOffset( ZoneOffset.UTC );
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

        Optional<Instant> savedLastPollTime = lastFutureActionPoolExecutionTimeDao.getLastPollTime();

        Instant lastPollExecuted;
        if ( savedLastPollTime.isPresent() ) {
            lastPollExecuted = savedLastPollTime.get();
        } else {
            lastPollExecuted = configs.getActionPoolEpoch();
            if( lastPollExecuted == null ) {
                lastPollExecuted = clock.instant().minus(2, ChronoUnit.HOURS);
            }
        }

        Instant now = clock.instant();
        List<String> uncheckedTimeSlots = computeTimeSlots(lastPollExecuted, now);
        for ( String timeSlot: uncheckedTimeSlots) {
            log.debug("Check time slot {}", timeSlot);
            actionDao.findActionsByTimeSlot(timeSlot).stream()
                    .filter(action -> now.isAfter(action.getNotBefore()))
                    .forEach(action -> this.scheduleOne(action, timeSlot));
        }

        lastFutureActionPoolExecutionTimeDao.updateLastPollTime( now );
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
