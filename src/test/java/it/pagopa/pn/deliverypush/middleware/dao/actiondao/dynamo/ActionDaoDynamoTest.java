package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.time.Instant;
import java.util.Optional;


class ActionDaoDynamoTest {
    @Mock
    private ActionEntityDao actionEntityDao;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private DynamoDbTable<FutureActionEntity> dynamoDbTableFutureAction;

    @Mock
    private ActionDaoDynamo dynamo;

    @BeforeEach
    void setup() {
        // Configura i mock per PnDeliveryPushConfigs
        PnDeliveryPushConfigs.ActionDao actionDao = new PnDeliveryPushConfigs.ActionDao();
        actionDao.setTableName("Action");
        PnDeliveryPushConfigs.FutureActionDao futureActionDao = new PnDeliveryPushConfigs.FutureActionDao();
        futureActionDao.setTableName("FutureAction");

        Mockito.when(pnDeliveryPushConfigs.getActionDao()).thenReturn(actionDao);
        Mockito.when(pnDeliveryPushConfigs.getFutureActionDao()).thenReturn(futureActionDao);
        Mockito.when(pnDeliveryPushConfigs.getActionTtlDays()).thenReturn("1095");

        Mockito.when(dynamoDbEnhancedClient.table(
                Mockito.any(),
                Mockito.any(TableSchema.class))
        ).thenReturn(dynamoDbTableFutureAction);

        //TableSchema<FutureActionEntity> tableSchema = TableSchema.fromClass(FutureActionEntity.class);

        // Inizializza l'istanza di ActionDaoDynamo con i mock
        dynamo = new ActionDaoDynamo( actionEntityDao, dynamoDbEnhancedClient, pnDeliveryPushConfigs);
    }

    @Test
    @ExtendWith(SpringExtension.class)
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
    @ExtendWith(SpringExtension.class)
    void unSchedule() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);

        dynamo.unScheduleFutureAction(action, timeslot);

        Mockito.verify(dynamoDbTableFutureAction, Mockito.times(1)).updateItem(Mockito.any(UpdateItemEnhancedRequest.class));
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

}