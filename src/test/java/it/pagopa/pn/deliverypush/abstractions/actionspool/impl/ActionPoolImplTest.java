package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.LastPollForFutureActions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

public class ActionPoolImplTest {

    TestActionsPoolImpl service;

    @Mock
    ActionDao actionDao;

    @Mock
    LastPollForFutureActionsDao lastPollForFutureActionsDao;

    @Mock
    MomProducer<ActionEvent> actionsQueue;

    @Mock
    Clock clock;

    @Test
    void pollForFutureActionsTestWithPrecedentExcecution() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //GIVEN
        actionsQueue = Mockito.mock(MomProducer.class);
        actionDao = Mockito.mock(ActionDao.class);
        clock = Mockito.mock(Clock.class);
        lastPollForFutureActionsDao = Mockito.mock(LastPollForFutureActionsDao.class);

        service = new TestActionsPoolImpl(actionsQueue, actionDao, clock, lastPollForFutureActionsDao);

        Mockito.when(clock.instant()).thenReturn(Instant.now());
        Instant registeredTime = Instant.now().minus(2, ChronoUnit.HOURS);
        LastPollForFutureActions lastPoll = LastPollForFutureActions.builder()
                .lastPollExecuted(registeredTime)
                .build();
        Mockito.when(lastPollForFutureActionsDao.getLastPollForFutureActionsById()).thenReturn(java.util.Optional.ofNullable(lastPoll));
        Mockito.doNothing().when(lastPollForFutureActionsDao).updateLastPollForFutureActions(Mockito.any(LastPollForFutureActions.class));

        //WHEN
        service.pollForFutureActions();

        //THEN
        Mockito.verify(actionDao, Mockito.times(121)).findActionsByTimeSlot(anyString());
    }

    @Test
    void pollForFutureActionsTestNoPrecedentExecutions() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //GIVEN
        actionsQueue = Mockito.mock(MomProducer.class);
        actionDao = Mockito.mock(ActionDao.class);
        clock = Mockito.mock(Clock.class);
        lastPollForFutureActionsDao = Mockito.mock(LastPollForFutureActionsDao.class);

        service = new TestActionsPoolImpl(actionsQueue, actionDao, clock, lastPollForFutureActionsDao);

        Mockito.when(clock.instant()).thenReturn(Instant.now().minus(2, ChronoUnit.HOURS));
        Mockito.doNothing().when(lastPollForFutureActionsDao).updateLastPollForFutureActions(Mockito.any(LastPollForFutureActions.class));

        //WHEN
        service.pollForFutureActions();

        //THEN
        Mockito.verify(actionDao, Mockito.times(121)).findActionsByTimeSlot(anyString());


    }

    private static class TestActionsPoolImpl extends ActionsPoolImpl {

        public TestActionsPoolImpl(MomProducer<ActionEvent> actionsQueue, ActionDao actionDao, Clock clock, LastPollForFutureActionsDao lastPollForFutureActionsDao) {
            super(actionsQueue, actionDao, clock, lastPollForFutureActionsDao);
        }

        @Override
        public Optional<Action> loadActionById(String actionId) {
            return super.loadActionById(actionId);
        }
    }

}
