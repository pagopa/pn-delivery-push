package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class ActionEntityDaoDynamoTest {

    private ActionEntityDaoDynamo actionEntityDaoDynamo;
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private PnDeliveryPushConfigs cfg;

    @BeforeEach
    public void setup(){
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        dynamoDbEnhancedClient = Mockito.mock(DynamoDbEnhancedClient.class);

        Mockito.when(cfg.getActionDao()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ActionDao.class));

        actionEntityDaoDynamo = new ActionEntityDaoDynamo(dynamoDbEnhancedClient, cfg);
    }

    @Test
    void preparePutIfAbsent() {
        Assertions.assertDoesNotThrow(() -> actionEntityDaoDynamo.preparePutIfAbsent(new ActionEntity()));

    }

    @Test
    void putIfAbsent() {
        ActionEntity actionEntity = new ActionEntity();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> actionEntityDaoDynamo.putIfAbsent(actionEntity));
    }
}