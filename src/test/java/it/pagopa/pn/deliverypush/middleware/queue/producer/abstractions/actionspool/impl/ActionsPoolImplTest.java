package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class ActionsPoolImplTest {

    private MomProducer<ActionEvent> actionsQueue;

    private ActionService actionService;

    private Clock clock;


    private PnDeliveryPushConfigs configs;
    private ActionsPoolImpl actionsPool;
    private Duration lockAtMostFor;
    private Duration timeToBreak;
    private FeatureEnabledUtils featureEnabledUtils;

    @BeforeEach
    void setup() { 
        LockAssert.TestHelper.makeAllAssertsPass(true);
        actionService = Mockito.mock(ActionService.class);
        clock = Mockito.mock(Clock.class);
        configs = Mockito.mock(PnDeliveryPushConfigs.class);
        lockAtMostFor = Duration.ofSeconds(600);
        timeToBreak = Duration.ofSeconds(10);
        actionsQueue = Mockito.mock(MomProducer.class);
        featureEnabledUtils = Mockito.mock(FeatureEnabledUtils.class);

        actionsPool = new ActionsPoolImpl(actionsQueue, actionService, clock, configs, featureEnabledUtils, lockAtMostFor, timeToBreak);
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

        // performance improvement disabled
        Mockito.when(clock.instant()).thenReturn(now);

        //THEN
        Mockito.verify(actionsQueue).push(Mockito.any(ActionEvent.class));
    }

    @Test
    void pollForFutureActionsNoAction() {
        //GIVEN
        final Instant now = Instant.now();
        Instant lastPool = now.minus(Duration.ofMinutes(10));

        Mockito.when(clock.instant()).thenReturn(now);

        List<String> timeSlots = actionsPool.computeTimeSlots(lastPool, now);
        final String timeSlot = timeSlots.get(0);

        //THEN
        Mockito.verify(actionsQueue, Mockito.never()).push(Mockito.any(ActionEvent.class));
    }

    @Test
    void pollForFutureActionsCloseToLookAtMostFor() {
        lockAtMostFor = Duration.ofMillis(10);
        timeToBreak = Duration.ofMillis(1);
        actionsPool = new ActionsPoolImpl(actionsQueue, actionService, clock, configs, featureEnabledUtils, lockAtMostFor, timeToBreak);

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

        Mockito.when(clock.instant()).thenReturn(Instant.now().minus(Duration.ofSeconds(10)));

        List<String> timeSlots = actionsPool.computeTimeSlots(lastPool, now);
        final String timeSlot = timeSlots.get(0);

        //THEN
        Mockito.verify(actionsQueue, Mockito.never()).push(Mockito.any(ActionEvent.class));
    }
}