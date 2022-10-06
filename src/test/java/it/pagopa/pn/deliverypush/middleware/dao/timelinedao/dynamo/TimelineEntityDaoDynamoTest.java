package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

class TimelineEntityDaoDynamoTest {

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private PnDeliveryPushConfigs cfg;

    private TimelineEntityDaoDynamo dynamo;

    @BeforeEach
    void setUp() {
        /*
        dynamoDbEnhancedClient = Mockito.mock(DynamoDbEnhancedClient.class);
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        dynamo = new TimelineEntityDaoDynamo(dynamoDbEnhancedClient, cfg);
        */
    }

    @Test
    void put() {
        //dynamo.put(TimelineElementEntity.builder().build());
    }

    @Test
    void get() {
    }

    @Test
    void delete() {
    }

    @Test
    void findByIun() {
    }

    @Test
    void searchByIunAndElementId() {
    }

    @Test
    void deleteByIun() {
    }

    @Test
    void putIfAbsent() {
    }
}