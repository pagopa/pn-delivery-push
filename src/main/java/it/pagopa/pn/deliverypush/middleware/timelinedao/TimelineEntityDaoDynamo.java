package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.deliverypush.middleware.model.notification.TimelineElementEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

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
}
