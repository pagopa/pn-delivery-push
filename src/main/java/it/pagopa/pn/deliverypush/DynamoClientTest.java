package it.pagopa.pn.deliverypush;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.deliverypush.middleware.model.notification.TimelineElementEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

@Component
public class DynamoClientTest {
    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    static final TableSchema<TimelineElementEntity> CUSTOMER_TABLE_SCHEMA =
            TableSchema.builder(TimelineElementEntity.class)
                    .newItemSupplier(TimelineElementEntity::new)
                    .addAttribute(String.class, a -> a.name("iun")
                            .getter(TimelineElementEntity::getIun)
                            .setter(TimelineElementEntity::setIun)
                            .tags(primaryPartitionKey()))
                    .addAttribute(String.class, a -> a.name("timeline_element_id")
                            .getter(TimelineElementEntity::getTimelineElementId)
                            .setter(TimelineElementEntity::setTimelineElementId)
                            .tags(primarySortKey()))
                    .addAttribute(String.class, a -> a.name("details")
                            .getter(TimelineElementEntity::getDetails)
                            .setter(TimelineElementEntity::setDetails))
                    .addAttribute(TimelineElementCategory.class, a -> a.name("category")
                            .getter(TimelineElementEntity::getCategory)
                            .setter(TimelineElementEntity::setCategory))
                    .addAttribute(String.class, a -> a.name("legalfactid")
                            .getter(TimelineElementEntity::getLegalFactId)
                            .setter(TimelineElementEntity::setLegalFactId))
                    .build();

    public DynamoClientTest(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.testMethod();
    }
    public void testMethod(){
        System.out.println("Client is "+ dynamoDbEnhancedClient);

        DynamoDbTable<TimelineElementEntity> table = dynamoDbEnhancedClient.table(TimelineElementEntity.TIMELINE_TABLE_NAME, CUSTOMER_TABLE_SCHEMA);
        
        // GetItem
        TimelineElementEntity customer = table.getItem(
                    Key.builder()
                        .partitionValue("senderId1-2022030218192")
                        .sortValue("senderId1-2022030218192_start")
                        .build());
        
        System.out.println("customer " + customer);
    }
}
