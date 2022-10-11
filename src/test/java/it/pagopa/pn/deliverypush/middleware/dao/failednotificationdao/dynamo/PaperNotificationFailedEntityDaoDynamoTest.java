package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class PaperNotificationFailedEntityDaoDynamoTest {

    @Mock
    DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    PnDeliveryPushConfigs cfg;

    private PaperNotificationFailedEntityDaoDynamo daoDynamo;

    @BeforeEach
    void setUp() {
        dynamoDbEnhancedClient = Mockito.mock(DynamoDbEnhancedClient.class);
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        // = new PaperNotificationFailedEntityDaoDynamo(dynamoDbEnhancedClient, cfg);
    }

    @Test
    void findByRecipientId() {
    }

    @Test
    void putIfAbsent() {
    }
}