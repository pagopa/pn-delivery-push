package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ActionService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;

class ActionsPoolImplTest {

    private ActionService actionService;
    private ActionsPoolImpl actionsPool;

    @BeforeEach
    void setup() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        actionService = Mockito.mock(ActionService.class);
        actionsPool = new ActionsPoolImpl( actionService);
    }

    @Test
    void addOnlyAction() {
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
        actionsPool.addOnlyAction(action);
        //THEN
        Mockito.verify(actionService).addOnlyActionIfAbsent(Mockito.any(Action.class));
    }

    @Test
    void unscheduleFutureAction() {
        //GIVEN
        final Instant now = Instant.now();
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.minus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .timeslot("2023-10-01T12:00")
                .build();
        Mockito.when(actionService.getActionById(Mockito.any(String.class)))
                .thenReturn(java.util.Optional.of(action));
        //WHEN
        actionsPool.unscheduleFutureAction("001");
        //THEN
        Mockito.verify(actionService).unSchedule(Mockito.eq(action), Mockito.any(String.class));
    }

}