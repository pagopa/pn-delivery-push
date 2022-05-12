package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
public class ActionEntityDaoDynamo extends AbstractDynamoKeyValueStore<ActionEntity> implements ActionEntityDao {

    protected ActionEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(ActionEntity.class)));
    }
 
    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getActionDao().getTableName();
    }
    
    @Override
    public void putIfAbsent(ActionEntity value) {
        throw new UnsupportedOperationException();
    }
}
