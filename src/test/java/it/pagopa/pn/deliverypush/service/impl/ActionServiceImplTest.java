package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ActionServiceImplTest {

    @Mock
    private ActionDao actionDao;
    @Mock
    private FeatureEnabledUtils featureEnabledUtils;
    
    private ActionServiceImpl actionService;

    @BeforeEach
    public void setup() {
        actionDao = Mockito.mock(ActionDao.class);
        actionService = new ActionServiceImpl(actionDao, featureEnabledUtils);
    }

    @Test
    void addActionAndFutureActionIfAbsent() {
        Action action = buildAction();
        String time = "2021-09-16T15:24:00.00Z";

        actionService.addActionAndFutureActionIfAbsent(action, time);

        Mockito.verify(actionDao, Mockito.times(1)).addActionAndFutureActionIfAbsent(action, time);
    }


    @Test
    void addOnlyActionIfAbsent() {
        Action action = buildAction();
        String time = "2021-09-16T15:24:00.00Z";

        actionService.addOnlyActionIfAbsent(action);

        Mockito.verify(actionDao, Mockito.times(1)).addOnlyActionIfAbsent(action);
    }

    @Test
    void addOnlyAction() {
        Action action = buildAction();

        actionService.addOnlyAction(action);

        Mockito.verify(actionDao, Mockito.times(1)).addOnlyAction(action);
    }

    @Test
    void getActionById() {

        Action action = buildAction();

        Mockito.when(actionDao.getActionById("002")).thenReturn(Optional.of(action));

        Optional<Action> actual = actionService.getActionById("002");

        Assertions.assertEquals(action, actual.get());
    }

    @Test
    void findActionsByTimeSlot() {
        Action action = buildAction();
        String time = "2021-09-16T15:24:00.00Z";

        List<Action> actionList = new ArrayList<>();
        actionList.add(action);

        Mockito.when(actionDao.findActionsByTimeSlot(time)).thenReturn(actionList);

        List<Action> actualList = actionService.findActionsByTimeSlot(time);

        Assertions.assertEquals(actionList, actualList);

    }

    @Test
    void unSchedule() {
        Action action = buildAction();
        String time = "2021-09-16T15:24:00.00Z";
        Mockito.when(featureEnabledUtils.isPerformanceImprovementEnabled(action.getNotBefore())).thenReturn( false);
        actionService.unSchedule(action, time);

        Mockito.verify(actionDao, Mockito.times(1)).unSchedule(action, time);
    }

    @Test
    void unScheduleFeatureFlagEnabled() {
        Action action = buildAction();
        String time = "2021-09-16T15:24:00.00Z";
        Mockito.when(featureEnabledUtils.isPerformanceImprovementEnabled(action.getNotBefore())).thenReturn( true);
        actionService.unSchedule(action, time);

        Mockito.verify(actionDao, Mockito.never()).unSchedule(action, time);
    }
    
    private Action buildAction() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return Action.builder()
                .recipientIndex(0)
                .iun("001")
                .type(ActionType.ANALOG_WORKFLOW)
                .actionId("002")
                .notBefore(instant)
                .build();
    }
}