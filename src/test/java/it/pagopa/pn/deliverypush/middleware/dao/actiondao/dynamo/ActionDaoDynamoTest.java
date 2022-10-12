package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.FutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class ActionDaoDynamoTest {

    @Mock
    private ActionEntityDao actionEntityDao;

    @Mock
    private FutureActionEntityDao futureActionEntityDao;

    @Mock
    private DtoToEntityActionMapper dtoToEntityActionMapper;

    @Mock
    private DtoToEntityFutureActionMapper dtoToEntityFutureActionMapper;

    @Mock
    private EntityToDtoActionMapper entityToDtoActionMapper;

    @Mock
    private EntityToDtoFutureActionMapper entityToDtoFutureActionMapper;

    @Mock
    private ActionDaoDynamo dynamo;

    @BeforeEach
    void setup() {
        actionEntityDao = Mockito.mock(ActionEntityDao.class);
        futureActionEntityDao = Mockito.mock(FutureActionEntityDao.class);
        dtoToEntityActionMapper = Mockito.mock(DtoToEntityActionMapper.class);
        dtoToEntityFutureActionMapper = Mockito.mock(DtoToEntityFutureActionMapper.class);
        entityToDtoActionMapper = Mockito.mock(EntityToDtoActionMapper.class);
        entityToDtoFutureActionMapper = Mockito.mock(EntityToDtoFutureActionMapper.class);
        dynamo = new ActionDaoDynamo(actionEntityDao, futureActionEntityDao, dtoToEntityActionMapper, dtoToEntityFutureActionMapper, entityToDtoActionMapper, entityToDtoFutureActionMapper);
    }

    @Test
    void addAction() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        FutureActionEntity futureActionEntity = buildFutureActionEntity(action, timeslot);

        Mockito.when(dtoToEntityActionMapper.dtoToEntity(action)).thenReturn(actionEntity);
        Mockito.when(dtoToEntityFutureActionMapper.dtoToEntity(action, timeslot)).thenReturn(futureActionEntity);

        dynamo.addAction(action, timeslot);

        Mockito.verify(actionEntityDao, Mockito.times(1)).put(actionEntity);
        Mockito.verify(futureActionEntityDao, Mockito.times(1)).put(futureActionEntity);
    }

    @Test
    void getActionById() {
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        Key keyToSearch = Key.builder()
                .partitionValue("2")
                .build();

        Mockito.when(entityToDtoActionMapper.entityToDto(actionEntity)).thenReturn(action);
        Mockito.when(actionEntityDao.get(keyToSearch)).thenReturn(Optional.of(actionEntity));

        Optional<Action> opt = dynamo.getActionById("2");

        Assertions.assertEquals(ActionType.ANALOG_WORKFLOW, opt.get().getType());
    }

    @Test
    void findActionsByTimeSlot() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        FutureActionEntity futureActionEntity = buildFutureActionEntity(action, timeslot);

        Set<FutureActionEntity> entities = new HashSet<>();
        entities.add(futureActionEntity);

        Mockito.when(futureActionEntityDao.findByTimeSlot(timeslot)).thenReturn(entities);

        List<Action> actionList = dynamo.findActionsByTimeSlot(timeslot);

        Assertions.assertEquals(1, actionList.size());
    }

    @Test
    void unSchedule() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);

        Key keyToDelete = buildKey(action, timeslot);

        dynamo.unSchedule(action, timeslot);

        Mockito.verify(futureActionEntityDao, Mockito.times(1)).delete(keyToDelete);
    }

    private Action buildAction(ActionType type) {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        return Action.builder()
                .iun("01")
                .actionId("02")
                .notBefore(instant)
                .type(type)
                .recipientIndex(3)
                .build();
    }

    private ActionEntity buildActionEntity(Action dto) {
        ActionEntity.ActionEntityBuilder builder = ActionEntity.builder()
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .iun(dto.getIun());
        return builder.build();
    }

    private FutureActionEntity buildFutureActionEntity(Action dto, String timeSlot) {
        FutureActionEntity.FutureActionEntityBuilder builder = FutureActionEntity.builder()
                .timeSlot(timeSlot)
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .iun(dto.getIun());
        return builder.build();
    }

    private Key buildKey(Action action, String timeSlot) {
        return Key.builder()
                .partitionValue(timeSlot)
                .sortValue(action.getActionId())
                .build();
    }

    private Action buildActionDto(ActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .iun(entity.getIun());
        return builder.build();
    }

    private Action buildFutureDto(FutureActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .iun(entity.getIun());
        return builder.build();
    }
}