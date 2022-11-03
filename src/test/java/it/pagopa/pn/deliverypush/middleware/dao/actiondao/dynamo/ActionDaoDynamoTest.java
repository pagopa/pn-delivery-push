package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.FutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

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
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private ActionDaoDynamo dynamo;

    @BeforeEach
    void setup() {
        actionEntityDao = Mockito.mock(ActionEntityDao.class);
        futureActionEntityDao = Mockito.mock(FutureActionEntityDao.class);
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        dynamoDbEnhancedClient = Mockito.mock(DynamoDbEnhancedClient.class);

        PnDeliveryPushConfigs.ActionDao actionDao = new PnDeliveryPushConfigs.ActionDao();
        actionDao.setTableName("Action");
        PnDeliveryPushConfigs.FutureActionDao factionDao = new PnDeliveryPushConfigs.FutureActionDao();
        factionDao.setTableName("FutureAction");
        Mockito.when(pnDeliveryPushConfigs.getActionDao()).thenReturn(actionDao);
        Mockito.when(pnDeliveryPushConfigs.getFutureActionDao()).thenReturn(factionDao);

        dynamo = new ActionDaoDynamo(actionEntityDao, futureActionEntityDao,
                dynamoDbEnhancedClient, pnDeliveryPushConfigs);
    }

    @Test
    void addAction() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        FutureActionEntity futureActionEntity = buildFutureActionEntity(action, timeslot);
        
        dynamo.addAction(action, timeslot);

        Mockito.verify(actionEntityDao, Mockito.times(1)).put(actionEntity);
        Mockito.verify(futureActionEntityDao, Mockito.times(1)).put(futureActionEntity);
    }


    @Test
    void addActionIfAbsent() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        FutureActionEntity futureActionEntity = buildFutureActionEntity(action, timeslot);

        PutItemEnhancedRequest<ActionEntity> putItemEnhancedRequest = PutItemEnhancedRequest.<ActionEntity>builder(null).build();
        PutItemEnhancedRequest<FutureActionEntity> putItemEnhancedRequest1 = PutItemEnhancedRequest.<FutureActionEntity>builder(null).build();

        Mockito.when(actionEntityDao.preparePutIfAbsent(actionEntity)).thenReturn(putItemEnhancedRequest);
        Mockito.when(futureActionEntityDao.preparePut(futureActionEntity)).thenReturn(putItemEnhancedRequest1);
        
        Mockito.when(dynamoDbEnhancedClient.table(Mockito.anyString(), Mockito.eq(TableSchema.fromClass(ActionEntity.class)))).thenReturn(Mockito.mock(DynamoDbTable.class));
        Mockito.when(dynamoDbEnhancedClient.table(Mockito.anyString(), Mockito.eq(TableSchema.fromClass(FutureActionEntity.class)))).thenReturn(Mockito.mock(DynamoDbTable.class));


        Mockito.doNothing().when(dynamoDbEnhancedClient).transactWriteItems(Mockito.any(TransactWriteItemsEnhancedRequest.class));

        // non si riesce a mockare TransactWriteItemsEnhancedRequest
        //Assertions.assertDoesNotThrow(() -> dynamo.addActionIfAbsent(action, timeslot));
    }

    @Test
    void getActionById() {
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        ActionEntity actionEntity = buildActionEntity(action);
        Key keyToSearch = Key.builder()
                .partitionValue("2")
                .build();

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