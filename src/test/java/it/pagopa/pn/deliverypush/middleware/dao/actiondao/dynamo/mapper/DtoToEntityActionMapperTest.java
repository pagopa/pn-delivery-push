package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class DtoToEntityActionMapperTest {

    @Test
    void dtoToEntity() {
        DtoToEntityActionMapper mapper = new DtoToEntityActionMapper();
        Action action = buildAction();
        ActionEntity expected = buildActionEntity();

        ActionEntity actual = mapper.dtoToEntity(action);

        Assertions.assertEquals(expected, actual);
    }

    private Action buildAction() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return Action.builder()
                .iun("001")
                .actionId("002")
                .notBefore(instant)
                .recipientIndex(1)
                .type(ActionType.ANALOG_WORKFLOW)
                .timelineId("2021-09-16T15:24:00.00Z")
                .build();
    }

    private ActionEntity buildActionEntity() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return ActionEntity.builder()
                .iun("001")
                .actionId("002")
                .notBefore(instant)
                .recipientIndex(1)
                .type(ActionType.ANALOG_WORKFLOW)
                .timelineId("2021-09-16T15:24:00.00Z")
                .build();
    }
}