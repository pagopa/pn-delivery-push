package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.deliverypush.abstractions.actionspool.LastPollForFutureActions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.lang.reflect.Method;
import java.time.Instant;

public class ActionPoolImplTest {

    @Mock
    ActionsPoolImpl service;
    @Autowired
    WebTestClient webTestClient;

    @Test
    void pollForFutureActionsTest() throws NoSuchMethodException {
        Instant registeredTime = Instant.now();
        LastPollForFutureActions lastPoll = LastPollForFutureActions.builder()
                .lastPollKey(1L)
                .lastPollExecuted(registeredTime)
                .build();
        Method method = service.getClass().getDeclaredMethod("pollForFutureActions",LastPollForFutureActions.class);
        method.setAccessible(true);
        Mockito.when(method(Mockito.any(LastPollForFutureActions.class)));
        
    }
}
