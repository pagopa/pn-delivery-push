package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.FutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;

import java.util.HashSet;
import java.util.Set;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
public class FutureActionEntityDaoDynamo  extends AbstractDynamoKeyValueStore<FutureActionEntity>  implements FutureActionEntityDao {

    protected FutureActionEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(FutureActionEntity.class)));
    }

    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getFutureActionDao().getTableName();
    }
    
    @Override
    public void putIfAbsent(FutureActionEntity value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<FutureActionEntity> findByTimeSlot(String timeSlot) {
        Key hashKey = Key.builder().partitionValue(timeSlot).build();
        QueryConditional queryByHashKey = keyEqualTo( hashKey );
        PageIterable<FutureActionEntity> futureActionElementsPage = table.query( queryByHashKey );

        Set<FutureActionEntity> set = new HashSet<>();
        futureActionElementsPage.stream().forEach(pages -> set.addAll(pages.items()));

        return set;
    }

    @Override
    public TransactPutItemEnhancedRequest<FutureActionEntity> preparePut(FutureActionEntity value) {

        return TransactPutItemEnhancedRequest.builder( FutureActionEntity.class )
                .item( value )
                .build();
    }
}
