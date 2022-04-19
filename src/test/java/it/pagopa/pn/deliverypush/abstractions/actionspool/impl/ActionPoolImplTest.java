package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.actiondao.LastPollForFutureActionsDao;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

class ActionPoolImplTest {

    private TestActionsPoolImpl service;
    private ActionDao actionDao;
    private LastPollForFutureActionsDao lastPollForFutureActionsDao;
    private Clock clock;
    private PnDeliveryPushConfigs configs;

    @BeforeEach
    void setup() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        
        MomProducer<ActionEvent> actionsQueue = Mockito.mock(MomProducer.class);
        actionDao = Mockito.mock(ActionDao.class);
        clock = Mockito.mock(Clock.class);
        lastPollForFutureActionsDao = Mockito.mock(LastPollForFutureActionsDao.class);
        configs = Mockito.mock( PnDeliveryPushConfigs.class );

        service = new TestActionsPoolImpl(actionsQueue, actionDao, clock, lastPollForFutureActionsDao, configs);
    }

    @Test
    void loadLastExecutionTimeFromStorage() {

        //GIVEN
        Instant now = Instant.now();
        Instant registeredTime = Instant.now().minus(1, ChronoUnit.HOURS);

        Mockito.when(clock.instant()).thenReturn( now );
        Mockito.when( lastPollForFutureActionsDao.getLastPollTime() )
                .thenReturn(Optional.ofNullable(registeredTime));
        Mockito.doNothing().when(lastPollForFutureActionsDao).updateLastPollTime(Mockito.any(Instant.class));

        //WHEN
        service.pollForFutureActions();

        //THEN
        Mockito.verify(actionDao, Mockito.times(61)).findActionsByTimeSlot(anyString());
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
        Mockito.verify(actionDao, Mockito.times(46)).findActionsByTimeSlot(anyString());
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
        Mockito.verify(actionDao, Mockito.times(121)).findActionsByTimeSlot(anyString());
    }

    private static class TestActionsPoolImpl extends ActionsPoolImpl {

        public TestActionsPoolImpl(
                MomProducer<ActionEvent> actionsQueue,
                ActionDao actionDao,
                Clock clock,
                LastPollForFutureActionsDao lastPollForFutureActionsDao,
                PnDeliveryPushConfigs configs
        ) {
            super(actionsQueue, actionDao, clock, lastPollForFutureActionsDao, configs);
        }

        @Override
        public Optional<Action> loadActionById(String actionId) {
            return super.loadActionById(actionId);
        }
    }

}
