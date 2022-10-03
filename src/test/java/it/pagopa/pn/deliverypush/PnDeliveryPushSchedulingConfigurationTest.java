package it.pagopa.pn.deliverypush;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class PnDeliveryPushSchedulingConfigurationTest {

    private PnDeliveryPushSchedulingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new PnDeliveryPushSchedulingConfiguration();
    }

    @Test
    void lockProvider() {
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        PnDeliveryPushConfigs cfg = new PnDeliveryPushConfigs();
        PnDeliveryPushConfigs.LastPollForFutureActionDao dao = new PnDeliveryPushConfigs.LastPollForFutureActionDao();
        dao.setLockTableName("Lock");
        cfg.setLastPollForFutureActionDao(dao);
        LockProvider provider = configuration.lockProvider(dynamoDB, cfg);
        Assertions.assertNotNull(provider);
    }
}