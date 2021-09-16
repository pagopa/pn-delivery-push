package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.abstractions.actionspool.LastPollForFutureActions;
import it.pagopa.pn.deliverypush.middleware.actiondao.cassandra.CassandraActionPool;
import it.pagopa.pn.deliverypush.middleware.actiondao.cassandra.CassandraLastPollForFutureActions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;

public class ActionPoolImplTest {

    ActionsPoolImpl service;

    @Mock
    ActionDao actionDao;

    @Mock
    LastPollForFutureActionsDao lastPollForFutureActionsDao;

    @Mock
    MomProducer<ActionEvent> actionsQueue;

    @Mock
    Clock clock;

    @Test
    void pollForFutureActionsTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        actionsQueue = Mockito.mock(MomProducer.class);
        actionDao = Mockito.mock(ActionDao.class);
        clock = Mockito.mock(Clock.class);
        lastPollForFutureActionsDao = Mockito.mock(LastPollForFutureActionsDao.class);

        service = new ActionsPoolImpl(actionsQueue, actionDao, clock, lastPollForFutureActionsDao);

        Instant registeredTime = Instant.now();
        LastPollForFutureActions lastPoll = LastPollForFutureActions.builder()
                .lastPollKey(1L)
                .lastPollExecuted(registeredTime)
                .build();

        Mockito.when(lastPollForFutureActionsDao.getLastPollForFutureActionsById(1L)).thenReturn(java.util.Optional.ofNullable(lastPoll));
        Method method = service.getClass().getDeclaredMethod("pollForFutureActions");
        method.setAccessible(true);
        System.out.println(method.invoke(service));

    }
}
