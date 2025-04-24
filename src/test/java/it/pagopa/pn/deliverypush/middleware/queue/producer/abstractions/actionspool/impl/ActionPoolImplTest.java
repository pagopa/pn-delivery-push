package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

class ActionPoolImplTest {

    private TestActionsPoolImpl service;
    private ActionService actionService;
    private FeatureEnabledUtils featureEnabledUtils;
    
    private Clock clock;
    private PnDeliveryPushConfigs configs;

    @BeforeEach
    void setup() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        
        MomProducer<ActionEvent> actionsQueue = Mockito.mock(MomProducer.class);
        actionService = Mockito.mock(ActionService.class);
        clock = Mockito.mock(Clock.class);
        configs = Mockito.mock( PnDeliveryPushConfigs.class );

        featureEnabledUtils = Mockito.mock(FeatureEnabledUtils.class);
                
        service = new TestActionsPoolImpl(actionsQueue, actionService, clock, configs, featureEnabledUtils);
    }

    @Test
    //Todo: aggiustare
    void loadLastExecutionTimeFromStorage() {

        //GIVEN
        Instant now = Instant.now();
        Instant registeredTime = Instant.now().minus(1, ChronoUnit.HOURS);

        Mockito.when(clock.instant()).thenReturn( now );
        //THEN
        
        //Viene verificato il numero di volte che è stato chiamato l'ActionService 
        ArgumentCaptor<String> timeSlotCaptor = ArgumentCaptor.forClass(String.class);

        List<String> timeSlots = timeSlotCaptor.getAllValues();
        String lastTimeSlot = timeSlots.get(60);

        ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);

        List<Instant> instantTimeSlot = instantArgumentCaptor.getAllValues();
        Instant lastInstantTimeslot = instantTimeSlot.get(60);

        Instant lastTimeSlotConverted = DateFormatUtils.getInstantFromString( lastTimeSlot, ActionsPoolImpl.TIMESLOT_PATTERN );
        
        //Viene verificato che l'ultimo timeSlot passato all'ActionService sia uguale all'ultimo timeslot passato a lastPollForFutureActionsDao
        Assertions.assertEquals( lastTimeSlotConverted, lastInstantTimeslot);
    }

    private static class TestActionsPoolImpl extends ActionsPoolImpl {

        public TestActionsPoolImpl(
                MomProducer<ActionEvent> actionsQueue,
                ActionService actionService,
                Clock clock,
                PnDeliveryPushConfigs configs,
                FeatureEnabledUtils featureEnabledUtils
        ) {
            super(actionsQueue, actionService, clock, configs, featureEnabledUtils, Duration.ofSeconds(600), Duration.ofSeconds(10));
        }
    }

}
