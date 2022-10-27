package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class FutureActionEntityDaoDynamoTest {

    private FutureActionEntityDaoDynamo actionEntityDaoDynamo;
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private PnDeliveryPushConfigs cfg;

    @BeforeEach
    public void setup(){
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        dynamoDbEnhancedClient = Mockito.mock(DynamoDbEnhancedClient.class);

        Mockito.when(cfg.getFutureActionDao()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.FutureActionDao.class));

        actionEntityDaoDynamo = new FutureActionEntityDaoDynamo(dynamoDbEnhancedClient, cfg);
    }

    @Test
    void putIfAbsent() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> actionEntityDaoDynamo.putIfAbsent(new FutureActionEntity()));
    }

    @Test
    void testPreparePut() {
        Assertions.assertDoesNotThrow(() -> actionEntityDaoDynamo.preparePut(new FutureActionEntity()));
    }
}