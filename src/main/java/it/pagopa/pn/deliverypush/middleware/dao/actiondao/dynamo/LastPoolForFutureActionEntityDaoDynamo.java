package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.LastPollForFutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.LastPollForFutureActionEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
public class LastPoolForFutureActionEntityDaoDynamo extends AbstractDynamoKeyValueStore<LastPollForFutureActionEntity> implements LastPollForFutureActionEntityDao {

    protected LastPoolForFutureActionEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(LastPollForFutureActionEntity.class)));
    }

    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getLastPollForFutureActionDao().getTableName();
    }

    @Override
    public void putIfAbsent(LastPollForFutureActionEntity value) {
        throw new UnsupportedOperationException();
    }
}
