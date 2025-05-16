package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.time.Instant;


class ActionDaoDynamoTest {
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
        dynamo = new ActionDaoDynamo( dynamoDbEnhancedClient, pnDeliveryPushConfigs);
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



}