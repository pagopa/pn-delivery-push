package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineCounterEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Component
@ConditionalOnProperty(name = TimelineCounterEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class TimelineCounterEntityDaoDynamo extends AbstractDynamoKeyValueStore<TimelineCounterEntity> implements TimelineCounterEntityDao {

    protected TimelineCounterEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(TimelineCounterEntity.class)));
    }
 
    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getTimelinecounterDao().getTableName();
    }


    @Override
    public TimelineCounterEntity getCounter(String timelineElementId) {
        return table.updateItem(createUpdateItemEnhancedRequest(timelineElementId));
    }

    protected UpdateItemEnhancedRequest<TimelineCounterEntity> createUpdateItemEnhancedRequest(String timelineElementId) {
        TimelineCounterEntity counterModel = new TimelineCounterEntity();
        counterModel.setTimelineElementId(timelineElementId);
        return UpdateItemEnhancedRequest
                .builder(TimelineCounterEntity.class)
                .item(counterModel)
                .build();
    }

    @Override
    public void putIfAbsent(TimelineCounterEntity value) throws PnIdConflictException {
       // nothing to do
    }
}
