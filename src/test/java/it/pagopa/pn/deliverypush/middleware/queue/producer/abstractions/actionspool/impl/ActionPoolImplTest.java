package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionsDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.ActionService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

class ActionPoolImplTest {

    private TestActionsPoolImpl service;
    private ActionService actionService;
    private LastPollForFutureActionsDao lastPollForFutureActionsDao;
    private Clock clock;
    private PnDeliveryPushConfigs configs;

    @BeforeEach
    void setup() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        
        MomProducer<ActionEvent> actionsQueue = Mockito.mock(MomProducer.class);
        actionService = Mockito.mock(ActionService.class);
        clock = Mockito.mock(Clock.class);
        lastPollForFutureActionsDao = Mockito.mock(LastPollForFutureActionsDao.class);
        configs = Mockito.mock( PnDeliveryPushConfigs.class );

        service = new TestActionsPoolImpl(actionsQueue, actionService, clock, lastPollForFutureActionsDao, configs);
    }

    @Test
    void loadLastExecutionTimeFromStorage() {

        //GIVEN
        Instant now = Instant.now();
        Instant registeredTime = Instant.now().minus(1, ChronoUnit.HOURS);

        Mockito.when(clock.instant()).thenReturn( now );
        Mockito.when( lastPollForFutureActionsDao.getLastPollTime() )
                .thenReturn(Optional.ofNullable(registeredTime));

        //WHEN
        service.pollForFutureActions();

        //THEN
        
        //Viene verificato il numero di volte che è stato chiamato l'ActionService 
        ArgumentCaptor<String> timeSlotCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(actionService, Mockito.times(61)).findActionsByTimeSlot(timeSlotCaptor.capture());

        List<String> timeSlots = timeSlotCaptor.getAllValues();
        String lastTimeSlot = timeSlots.get(60);

        ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);

        //Viene verificato il numero di volte che è stato chiamato il lastPollForFutureActionsDao
        Mockito.verify(lastPollForFutureActionsDao, Mockito.times(61)).updateLastPollTime(instantArgumentCaptor.capture());

        List<Instant> instantTimeSlot = instantArgumentCaptor.getAllValues();
        Instant lastInstantTimeslot = instantTimeSlot.get(60);

        Instant lastTimeSlotConverted = DateFormatUtils.getInstantFromString( lastTimeSlot, ActionsPoolImpl.TIMESLOT_PATTERN );
        
        //Viene verificato che l'ultimo timeSlot passato all'ActionService sia uguale all'ultimo timeslot passato a lastPollForFutureActionsDao
        Assertions.assertEquals( lastTimeSlotConverted, lastInstantTimeslot);
    }

    @Test
    void noStorageDefaultLastExecutionTime() {

        //GIVEN
        Instant lastFromConfig = Instant.ofEpochSecond( 65 );
        Instant now = Instant.ofEpochSecond( 65 + 60 * 45);

        Mockito.when(clock.instant()).thenReturn( now );
        Mockito.when( configs.getActionPoolEpoch() ).thenReturn( lastFromConfig );
        Mockito.doNothing().when(lastPollForFutureActionsDao).updateLastPollTime(Mockito.any(Instant.class));

        //WHEN
        service.pollForFutureActions();

        //THEN
        Mockito.verify(actionService, Mockito.times(46)).findActionsByTimeSlot(anyString());
    }

    @Test
    void noStorageLastExecutionTimeFromConfig() {

        //GIVEN
        Instant now = Instant.now();

        Mockito.when(clock.instant()).thenReturn( now );
        Mockito.doNothing().when(lastPollForFutureActionsDao).updateLastPollTime(Mockito.any(Instant.class));

        //WHEN
        service.pollForFutureActions();

        //THEN
        Mockito.verify(actionService, Mockito.times(121)).findActionsByTimeSlot(anyString());
    }

    private static class TestActionsPoolImpl extends ActionsPoolImpl {

        public TestActionsPoolImpl(
                MomProducer<ActionEvent> actionsQueue,
                ActionService actionService,
                Clock clock,
                LastPollForFutureActionsDao lastPollForFutureActionsDao,
                PnDeliveryPushConfigs configs
        ) {
            super(actionsQueue, actionService, clock, lastPollForFutureActionsDao, configs, Duration.ofSeconds(600), Duration.ofSeconds(10));
        }

        @Override
        public Optional<Action> loadActionById(String actionId) {
            return super.loadActionById(actionId);
        }
    }

}
