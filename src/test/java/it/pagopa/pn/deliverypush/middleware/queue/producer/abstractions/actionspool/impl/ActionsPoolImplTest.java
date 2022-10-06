package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionsDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ActionService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.ActionsPoolImpl.TIMESLOT_PATTERN;

class ActionsPoolImplTest {

    @Mock
    private MomProducer<ActionEvent> actionsQueue;

    @Mock
    private ActionService actionService;

    @Mock
    private Clock clock;

    @Mock
    private LastPollForFutureActionsDao lastFutureActionPoolExecutionTimeDao;

    @Mock
    private PnDeliveryPushConfigs configs;

    private ActionsPoolImpl actionsPool;

    @BeforeEach
    void setup() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        actionService = Mockito.mock(ActionService.class);
        clock = Mockito.mock(Clock.class);
        lastFutureActionPoolExecutionTimeDao = Mockito.mock(LastPollForFutureActionsDao.class);
        configs = Mockito.mock(PnDeliveryPushConfigs.class);
        actionsPool = new ActionsPoolImpl(actionsQueue, actionService, clock, lastFutureActionPoolExecutionTimeDao, configs);
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

        String now = "2022-09-14T06:25";
        Instant instantTimeSlot = DateFormatUtils.getInstantFromString(now, TIMESLOT_PATTERN);
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(Instant.now())
                .type(ActionType.ANALOG_WORKFLOW)
                .build();

        List<Action> actions = new ArrayList<>();
        actions.add(action);
        
        Mockito.when(lastFutureActionPoolExecutionTimeDao.getLastPollTime()).thenReturn(Optional.of(instantTimeSlot));
        Mockito.when(clock.instant()).thenReturn(instantTimeSlot);
        Mockito.when(actionService.findActionsByTimeSlot("001")).thenReturn(actions);
        
        actionsPool.pollForFutureActions();

        Assertions.assertSame(action, actionService.findActionsByTimeSlot("001").get(0));
    }

    private String computeTimeSlot(Instant instant) {
        OffsetDateTime nowUtc = instant.atOffset(ZoneOffset.UTC);
        int year = nowUtc.get(ChronoField.YEAR_OF_ERA);
        int month = nowUtc.get(ChronoField.MONTH_OF_YEAR);
        int day = nowUtc.get(ChronoField.DAY_OF_MONTH);
        int hour = nowUtc.get(ChronoField.HOUR_OF_DAY);
        int minute = nowUtc.get(ChronoField.MINUTE_OF_HOUR);
        return String.format("%04d-%02d-%02dT%02d:%02d", year, month, day, hour, minute);
    }

}