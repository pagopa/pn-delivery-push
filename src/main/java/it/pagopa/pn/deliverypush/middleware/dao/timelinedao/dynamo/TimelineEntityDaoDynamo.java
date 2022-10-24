package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBeginsWith;

@Component
@Slf4j
public class TimelineEntityDaoDynamo  extends AbstractDynamoKeyValueStore<TimelineElementEntity> implements TimelineEntityDao {

    public TimelineEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(TimelineElementEntity.class)));
    }

    private static String tableName( PnDeliveryPushConfigs cfg ) {
        return cfg.getTimelineDao().getTableName();
    }

    @Override
    public Set<TimelineElementEntity> findByIun(String iun) {
        Key hashKey = Key.builder().partitionValue(iun).build();
        QueryConditional queryByHashKey = keyEqualTo( hashKey );
        return pageIterableToSet(table.query( queryByHashKey ));
    }


    @Override
    public Set<TimelineElementEntity> searchByIunAndElementId(String iun, String elementId) {
        Key hashKey = Key.builder().partitionValue(iun).sortValue(elementId).build();
        QueryConditional queryByHashKey = sortBeginsWith( hashKey );
        return pageIterableToSet(table.query( queryByHashKey ));
    }

    private  Set<TimelineElementEntity> pageIterableToSet(PageIterable<TimelineElementEntity> timelineElementPages)
    {
        Set<TimelineElementEntity> set = new HashSet<>();
        timelineElementPages.stream().forEach(pages -> set.addAll(pages.items()));

        return set;
    }

    @Override
    public void deleteByIun(String iun) {
        findByIun(iun).forEach(entity ->{
            Key keyToDelete = Key.builder()
                    .partitionValue(entity.getIun())
                    .sortValue(entity.getTimelineElementId())
                    .build();
            delete(keyToDelete);
        });
    }

    @Override
    public void putIfAbsent(TimelineElementEntity value) throws PnIdConflictException {
        String expression = String.format(
                "%s(%s) AND %s(%s)",
                ATTRIBUTE_NOT_EXISTS,
                TimelineElementEntity.FIELD_IUN,
                ATTRIBUTE_NOT_EXISTS,
                TimelineElementEntity.FIELD_TIMELINE_ELEMENT_ID
        );
        
        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();
        
        PutItemEnhancedRequest<TimelineElementEntity> request = PutItemEnhancedRequest.builder( TimelineElementEntity.class )
                .item(value )
                .conditionExpression( conditionExpressionPut )
                .build();
        
        try {
            table.putItem(request);
        } catch (ConditionalCheckFailedException ex){
            log.warn("Conditional check exception on TimelineEntityDaoDynamo putIfAbsent timelineId=" + value.getTimelineElementId() + " exmessage={}" + ex.getMessage());
            throw new PnIdConflictException(
                    PnDeliveryPushExceptionCodes.ERROR_CODE_DUPLICATED_ITEMD,
                    Collections.singletonMap("timelineElementId", value.getTimelineElementId()),
                    ex
            );
        }
    }
}
