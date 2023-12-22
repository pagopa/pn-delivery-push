package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionsDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ActionService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ActionsPoolImplTest {

    private MomProducer<ActionEvent> actionsQueue;

    private ActionService actionService;

    private Clock clock;

    private LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao;

    private PnDeliveryPushConfigs configs;
    private ActionsPoolImpl actionsPool;
    private Duration lockAtMostFor;
    private Duration timeToBreak;

    @BeforeEach
    void setup() { 
        LockAssert.TestHelper.makeAllAssertsPass(true);
        actionService = Mockito.mock(ActionService.class);
        clock = Mockito.mock(Clock.class);
        lastFutureActionPoolExecutionTimeDao = Mockito.mock(LastPollForFutureActionsDao.class);
        configs = Mockito.mock(PnDeliveryPushConfigs.class);
        lockAtMostFor = Duration.ofSeconds(600);
        timeToBreak = Duration.ofSeconds(10);
        actionsQueue = Mockito.mock(MomProducer.class);
        
        actionsPool = new ActionsPoolImpl(actionsQueue, actionService, clock, lastFutureActionPoolExecutionTimeDao, configs, lockAtMostFor, timeToBreak);
    }

    @Test
    void scheduleFutureAction() {
        //GIVEN
        final Instant now = Instant.now();
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.minus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .build();
        
        //WHEN
        actionsPool.scheduleFutureAction(action);
        //THEN
        Mockito.verify(actionService).addActionAndFutureActionIfAbsent(Mockito.any(Action.class), Mockito.anyString());
    }
    
    @Test
    void loadActionById() {
        String actionId = "001";
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(Instant.now())
                .type(ActionType.ANALOG_WORKFLOW)
                .build();

        Mockito.when(actionService.getActionById(actionId)).thenReturn(Optional.of(action));

        Optional<Action> result = actionsPool.loadActionById(actionId);

        Assertions.assertEquals(result.get(), action);
    }

    @Test
    void pollForFutureActions() {
        //GIVEN
        final Instant now = Instant.now();
        Instant lastPool = now.minus(Duration.ofMinutes(10));
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.minus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .build();

        List<Action> actions = new ArrayList<>();
        actions.add(action);

        Mockito.when(lastFutureActionPoolExecutionTimeDao.getLastPollTime()).thenReturn(Optional.of(lastPool));
        Mockito.when(clock.instant()).thenReturn(now);
        
        List<String> timeSlots = actionsPool.computeTimeSlots(lastPool, now);
        final String timeSlot = timeSlots.get(0);
        Mockito.when(actionService.findActionsByTimeSlot(timeSlot)).thenReturn(actions);
        
        //WHEN
        actionsPool.pollForFutureActions();
        //THEN
        Mockito.verify(actionsQueue).push(Mockito.any(ActionEvent.class));
        Mockito.verify(actionService).unSchedule(action, timeSlot);
    }

    @Test
    void pollForFutureActionsNoAction() {
        //GIVEN
        final Instant now = Instant.now();
        Instant lastPool = now.minus(Duration.ofMinutes(10));

        Mockito.when(lastFutureActionPoolExecutionTimeDao.getLastPollTime()).thenReturn(Optional.of(lastPool));
        Mockito.when(clock.instant()).thenReturn(now);

        List<String> timeSlots = actionsPool.computeTimeSlots(lastPool, now);
        final String timeSlot = timeSlots.get(0);
        Mockito.when(actionService.findActionsByTimeSlot(timeSlot)).thenReturn(new ArrayList<>());

        //WHEN
        actionsPool.pollForFutureActions();
        //THEN
        Mockito.verify(actionsQueue, Mockito.never()).push(Mockito.any(ActionEvent.class));
        Mockito.verify(actionService, Mockito.never()).unSchedule(Mockito.any(), Mockito.anyString());
    }

    @Test
    void pollForFutureActionsCloseToLookAtMostFor() {
        lockAtMostFor = Duration.ofMillis(10);
        timeToBreak = Duration.ofMillis(1);
        actionsPool = new ActionsPoolImpl(actionsQueue, actionService, clock, lastFutureActionPoolExecutionTimeDao, configs, lockAtMostFor, timeToBreak);

        //GIVEN
        final Instant now = Instant.now();
        Instant lastPool = now.minus(Duration.ofMinutes(10));
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.minus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .build();

        List<Action> actions = new ArrayList<>();
        actions.add(action);

        Mockito.when(lastFutureActionPoolExecutionTimeDao.getLastPollTime()).thenReturn(Optional.of(lastPool));
        Mockito.when(clock.instant()).thenReturn(Instant.now().minus(Duration.ofSeconds(10)));

        List<String> timeSlots = actionsPool.computeTimeSlots(lastPool, now);
        final String timeSlot = timeSlots.get(0);
        Mockito.when(actionService.findActionsByTimeSlot(timeSlot)).thenReturn(actions);

        //WHEN
        actionsPool.pollForFutureActions();
        //THEN
        Mockito.verify(actionsQueue, Mockito.never()).push(Mockito.any(ActionEvent.class));
        Mockito.verify(actionService, Mockito.never()).unSchedule(action, timeSlot);
    }

    @Test
    void scheduleFutureActionBefore() {
        //GIVEN
        final Instant now = Instant.now();
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.minus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .build();



        //WHEN
        actionsPool.startActionOrScheduleFutureAction(action);
        //THEN
        Mockito.verify(actionService).addOnlyActionIfAbsent(Mockito.any(Action.class));
        Mockito.verify(actionsQueue).push(Mockito.any(ActionEvent.class));
    }

    @Test
    void scheduleFutureActionAfter() {
        //GIVEN
        final Instant now = Instant.now();
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.plus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .build();



        //WHEN
        actionsPool.startActionOrScheduleFutureAction(action);
        //THEN
        Mockito.verify(actionService).addActionAndFutureActionIfAbsent(Mockito.any(Action.class), Mockito.anyString());
        Mockito.verify(actionsQueue, Mockito.never()).push(Mockito.any(ActionEvent.class));
    }
}