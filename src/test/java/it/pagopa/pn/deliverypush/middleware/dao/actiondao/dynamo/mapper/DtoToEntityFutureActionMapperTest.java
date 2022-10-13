package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class DtoToEntityFutureActionMapperTest {

    @Test
    void dtoToEntity() {
        FutureActionEntity expected = buildFutureActionEntity();
        Action action = buildAction();
        DtoToEntityFutureActionMapper mapper = new DtoToEntityFutureActionMapper();
        FutureActionEntity actual = mapper.dtoToEntity(action, "1");
        Assertions.assertEquals(expected, actual);
    }

    private FutureActionEntity buildFutureActionEntity() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        return FutureActionEntity.builder()
                .timeSlot("1")
                .actionId("2")
                .notBefore(instant)
                .recipientIndex(3)
                .type(ActionType.ANALOG_WORKFLOW)
                .timelineId("4")
                .iun("5").build();
    }

    private Action buildAction() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        return Action.builder()
                .timelineId("4")
                .type(ActionType.ANALOG_WORKFLOW)
                .recipientIndex(3)
                .notBefore(instant)
                .iun("5")
                .actionId("2")
                .build();
    }
}