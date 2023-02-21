package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;

@Component
@Slf4j
public class ActionEntityDaoDynamo extends AbstractDynamoKeyValueStore<ActionEntity> implements ActionEntityDao {
    
    protected ActionEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(ActionEntity.class)));
    }
 
    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getActionDao().getTableName();
    }

    public TransactPutItemEnhancedRequest<ActionEntity> preparePutIfAbsent(ActionEntity value){
        String expression = String.format(
                "%s(%s)",
                ATTRIBUTE_NOT_EXISTS,
                ActionEntity.FIELD_ACTION_ID
        );

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        return TransactPutItemEnhancedRequest.builder( ActionEntity.class )
                .item( value )
                .conditionExpression( conditionExpressionPut )
                .build();
    }

    @Override
    public void putIfAbsent(ActionEntity value) {
        throw new UnsupportedOperationException();
    }
}
