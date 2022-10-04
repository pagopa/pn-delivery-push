package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionsDao;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
import java.util.UUID;

@Service
@Slf4j
public class ActionsPoolImpl implements ActionsPool {

    public static final String TIMESLOT_PATTERN = "yyyy-MM-dd'T'HH:mm";
    private final MomProducer<ActionEvent> actionsQueue;
    private final ActionService actionService;
    private final Clock clock;
    private final LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao;
    private final PnDeliveryPushConfigs configs;

    public ActionsPoolImpl(MomProducer<ActionEvent> actionsQueue, ActionService actionService,
                           Clock clock, LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao, PnDeliveryPushConfigs configs) {
        this.actionsQueue = actionsQueue;
        this.actionService = actionService;
        this.clock = clock;
        this.lastFutureActionPoolExecutionTimeDao = lastFutureActionPoolExecutionTimeDao;
        this.configs = configs;
    }

    @Override
    public void scheduleFutureAction(Action action) {
        if ( Instant.now().isAfter( action.getNotBefore() )) {
            action = action.toBuilder()
                        .notBefore( Instant.now().plusSeconds(1))
                        .build();
        }
        final String timeSlot = computeTimeSlot( action.getNotBefore() );
        action = action.toBuilder()
                .timeslot( timeSlot)
                .build();
        actionService.addAction( action, timeSlot);
    }

    @Override
    public void unscheduleFutureAction(String actionId) {
        Optional<Action> actionEntity = actionService.getActionById(actionId);
        if (actionEntity.isPresent() && actionEntity.get().getTimeslot() != null) {
            actionService.unSchedule(actionEntity.get(), actionEntity.get().getTimeslot());
        }
    }

    @Override
    public Optional<Action> loadActionById( String actionId) {
        return actionService.getActionById( actionId );
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

//    lockAtMostFor specifies how long the lock should be kept in case the executing node dies. You have to set lockAtMostFor to a value which 
//    is much longer than normal execution time  If the task takes longer than lockAtMostFor the resulting behavior may be unpredictable
//    (more than one process will effectively hold the lock).
//    lockAtLeastFor specifies minimum amount of time for which the lock should be kept. is to prevent execution from multiple nodes 
//    in case of really short tasks and clock difference between the nodes. Setting lockAtLeastFor we make sure it's not executed more than once in 1 minute
    @Scheduled( fixedDelay = 10 * 1000L )
    @SchedulerLock(name = "actionPoll", lockAtMostFor = "1m", lockAtLeastFor = "30s")
    protected void pollForFutureActions() {
        // To assert that the lock is held (prevents misconfiguration errors)
        LockAssert.assertLocked();

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
        log.debug("Action pool start poll {}", lastPollExecuted);

        Instant now = clock.instant();
        List<String> uncheckedTimeSlots = computeTimeSlots(lastPollExecuted, now);
        
        for ( String timeSlot: uncheckedTimeSlots) {
            actionService.findActionsByTimeSlot(timeSlot).stream()
                    .filter(action -> now.isAfter(action.getNotBefore()))
                    .forEach(action -> this.scheduleOne(action, timeSlot));

            Instant instantTimeSlot = DateFormatUtils.getInstantFromString( timeSlot, TIMESLOT_PATTERN);
            lastFutureActionPoolExecutionTimeDao.updateLastPollTime( instantTimeSlot );
        }

    }

    private void scheduleOne( Action action, String timeSlot) {
        try {
            log.info("Scheduling action {}", action );
            addToActionsQueue( action );
            actionService.unSchedule( action, timeSlot );
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
                        .eventId( UUID.randomUUID().toString()) // per alcuni actionId la dimensione superava gli 80 caratteri permessi per id delle code
                        .createdAt( clock.instant() )
                        .eventType( ActionEventType.ACTION_GENERIC.name() )
                        .build()
                )
                .payload( action )
                .build()
        );
    }
}
