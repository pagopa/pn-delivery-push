package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionsDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.CustomLog;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@CustomLog
public class ActionsPoolImpl implements ActionsPool {

    public static final String TIMESLOT_PATTERN = "yyyy-MM-dd'T'HH:mm";
    private final MomProducer<ActionEvent> actionsQueue;
    private final ActionService actionService;
    private final Clock clock;
    private final LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao;
    private final PnDeliveryPushConfigs configs;

    private Duration lockAtMostFor;
    private Duration timeToBreak;
    
    public ActionsPoolImpl(MomProducer<ActionEvent> actionsQueue, ActionService actionService,
                           Clock clock, LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao, PnDeliveryPushConfigs configs,
                           @Value("${lockAtMostFor}") Duration lockAtMostFor, @Value("${timeToBreak}") Duration timeToBreak) {
        this.actionsQueue = actionsQueue;
        this.actionService = actionService;
        this.clock = clock;
        this.lastFutureActionPoolExecutionTimeDao = lastFutureActionPoolExecutionTimeDao;
        this.configs = configs;
        this.lockAtMostFor = lockAtMostFor;
        this.timeToBreak = timeToBreak;
    }

    /**
     * Schedula la action sempre e comunque utilizzando la tabella futureAction qualsiasi sia il notBefore
     *
     * @param action da schedulare
     */
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
        
        actionService.addActionAndFutureActionIfAbsent(action, timeSlot);
    }
    
    /**
     * Schedula la action. Se la action si riferisce ad un istante passato (o molto vicino), si procede
     * a salvare solo il record di action senza future, e a inserire direttametne in coda.
     *
     * @param action da schedulare
     */
    @Override
    public void startActionOrScheduleFutureAction(Action action) {
        if(isPerformanceImprovementEnabled(action.getNotBefore())) {
            actionService.addOnlyAction(action);
        } else {
            boolean isFutureSchedule = Instant.now().plus(configs.getActionPoolBeforeDelay()).isBefore(action.getNotBefore());

            final String timeSlot = computeTimeSlot( action.getNotBefore() );
            action = action.toBuilder()
                    .timeslot( timeSlot)
                    .build();

            if (isFutureSchedule) {
                actionService.addActionAndFutureActionIfAbsent(action, timeSlot);
            }
            else {
                actionService.addOnlyActionIfAbsent(action);
                addToActionsQueue(action);
            }
        }
    }

    private boolean isPerformanceImprovementEnabled(Instant notBefore) {
        boolean isEnabled = false;
        Instant startDate = Instant.parse(configs.getPerformanceImprovementStartDate());
        Instant endDate = Instant.parse(configs.getPerformanceImprovementEndDate());
        if ( notBefore.compareTo(startDate) >= 0 && notBefore.compareTo(endDate) <= 0) {
            isEnabled = true;
        }
        return isEnabled;
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

    protected List<String> computeTimeSlots(Instant from, Instant to) {
        List<String> timeSlots = new ArrayList<>();
        Instant timeSlotStart = from.truncatedTo( ChronoUnit.MINUTES );
        //I timeslot restituiti arrivano al minuto appena prima a now
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
//    in case of really short tasks and clock difference between the nodes.
    @Scheduled( fixedDelayString = "${fixedDelayPool}" )
    @SchedulerLock(name = "actionPoll",  lockAtMostFor = "${lockAtMostFor}", lockAtLeastFor = "${lockAtLeastFor}")
    protected void pollForFutureActions() {
        try {
            // To assert that the lock is held (prevents misconfiguration errors)
            LockAssert.assertLocked();
            handleActionPool();
        }catch (Exception ex){
            log.fatal("Exception in actionPool", ex);
        }
    }

    private void handleActionPool() {
        //Viene presa la data di quando è avvenuto l'ultimo pool
        Instant lastPollExecuted = getLastPollExecuted();
        Instant start = clock.instant();
        log.debug("Action pool now={} lastPollExecuted={}", start,  lastPollExecuted);
        
        //Vengono calcolati i timeslot da parsare (a partire da lastPollExecuted fino a now)
        List<String> uncheckedTimeSlots = computeTimeSlots(lastPollExecuted, start);

        boolean toBreak = false;
        for ( String timeSlot: uncheckedTimeSlots) {
            List<Action> actionList = actionService.findActionsByTimeSlot(timeSlot);
            log.debug("timeSlot size is {}", actionList.size());
            
            for(Action action: actionList){
                if(!isPerformanceImprovementEnabled(action.getNotBefore())) {
                    /*Viene verificato se si sta andando oltre il lockAtMostFor, in quel caso per evitare che il lock si sblocchi quando un nodo sta ancora lavorando,
                    si esce semplicemente dal for */
                    Duration timeSpent = getTimeSpent(start);
                    Duration closeToLookAtMostFor = lockAtMostFor.minus(timeToBreak);

                    if(timeSpent.compareTo(closeToLookAtMostFor) < 0){
                        if(start.isAfter(action.getNotBefore())){
                            this.scheduleOne(action, timeSlot);
                        }
                    } else {
                        log.warn("Polling is interrupted because it is very close to lockAtMostFor");
                        toBreak = true;
                        break;
                    }
                }
            }
            
            /*Viene effettuato l'update lastPoolTime con l'ultimo timeslot ottenuto (NOTA, potrebbe anche essere un timeslot parsato in parte, dunque il prossimo nodo
            prenderà lo stesso timeslot con eventuali ulteriori action ancora non lavarate)*/
            Instant instantTimeSlot = DateFormatUtils.getInstantFromString( timeSlot, TIMESLOT_PATTERN);
            lastFutureActionPoolExecutionTimeDao.updateLastPollTime( instantTimeSlot );

            if (toBreak)
            {
                // nel caso non sia riuscito a completare tutti gli timeslot, esco in modo che la prossima iterazione parta da qui.
                log.warn("skipping remaing timeslots, last timeslotexecuted={}", timeSlot);
                break;
            }
        }

        Duration timeSpent = getTimeSpent(start);
        log.debug("Action pool end. Time spent is {} millis", timeSpent.toMillis());

        if(timeSpent.compareTo(lockAtMostFor) > 0){
            log.fatal("Time spent is greater then lockAtMostFor. Multiple nodes could schedule the same actions.");
        }
    }

    private Instant getLastPollExecuted() {
        Optional<Instant> savedLastPollTime = lastFutureActionPoolExecutionTimeDao.getLastPollTime();
        return getLastPollExecuted(savedLastPollTime);
    }

    private static Duration getTimeSpent(Instant start) {
        Instant end = Instant.now();
        return Duration.between(start, end);
    }

    private Instant getLastPollExecuted(Optional<Instant> savedLastPollTime) {
        Instant lastPollExecuted;
        if ( savedLastPollTime.isPresent() ) {
            lastPollExecuted = savedLastPollTime.get();
        } else {
            lastPollExecuted = configs.getActionPoolEpoch();
            if( lastPollExecuted == null ) {
                lastPollExecuted = clock.instant().minus(2, ChronoUnit.HOURS);
            }
        }
        return lastPollExecuted;
    }

    private void scheduleOne( Action action, String timeSlot) {
        log.debug("Scheduling action {}", action );
        
        addToActionsQueue( action );
        actionService.unSchedule( action, timeSlot );

        log.debug("Action with actionId={} added to queue and unscheduled", action.getActionId() );
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
