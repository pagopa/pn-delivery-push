package it.pagopa.pn.deliverypush.middleware.dao.timelineshedlockdao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

@Component
public class TimelineShedlockEntityDaoDynamo extends AbstractDynamoKeyValueStore<TimelineShedlockEntity> {

    protected TimelineShedlockEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(TimelineShedlockEntity.class)));
    }

    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getTimelineShedlockDao().getTableName();
    }

    public TimelineShedlockEntity getItem(String iun) {
        return table.getItem(GetItemEnhancedRequest.builder().key(Key.builder().partitionValue(iun).build()).build());
    }

    @Override
    public void putIfAbsent(TimelineShedlockEntity timelineShedlockEntity) throws PnIdConflictException {
        // nothing to do
    }
}
