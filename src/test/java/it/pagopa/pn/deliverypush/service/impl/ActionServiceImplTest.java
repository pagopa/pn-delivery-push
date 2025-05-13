package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class ActionServiceImplTest {

    @Mock
    private ActionDao actionDao;
    
    private ActionServiceImpl actionService;

    @BeforeEach
    public void setup() {
        actionDao = Mockito.mock(ActionDao.class);
        actionService = new ActionServiceImpl(actionDao);
    }

    @Test
    void addOnlyActionIfAbsent() {
        Action action = buildAction();

        actionService.addOnlyActionIfAbsent(action);

        Mockito.verify(actionDao, Mockito.times(1)).addOnlyActionIfAbsent(action);
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