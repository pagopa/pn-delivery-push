package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.deliverypush.middleware.model.notification.TimelineElementEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.HashSet;
import java.util.Set;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
public class TimelineEntityDaoDynamo  extends AbstractDynamoKeyValueStore<TimelineElementEntity> implements TimelineEntityDao<TimelineElementEntity,Key>{
    
    public TimelineEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        super(dynamoDbEnhancedClient.table(TimelineElementEntity.TIMELINE_TABLE_NAME, TableSchema.fromClass(TimelineElementEntity.class)));
    }

    @Override
    public Set<TimelineElementEntity> findByIun(String iun) {
        PageIterable<TimelineElementEntity> timelineElementPages = table.query(keyEqualTo(k -> k.partitionValue(iun)));

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
    public void putIfAbsent(TimelineElementEntity value) throws IdConflictException {
        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(iun) AND attribute_not_exists(timeline_id)")
                .build();
        
        PutItemEnhancedRequest<TimelineElementEntity> request = PutItemEnhancedRequest.builder( TimelineElementEntity.class )
                .item(value )
                .conditionExpression( conditionExpressionPut )
                .build();
        try {
            table.putItem(request);
        }catch (ConditionalCheckFailedException ex){
            throw new IdConflictException(value);
        }
    }
}
