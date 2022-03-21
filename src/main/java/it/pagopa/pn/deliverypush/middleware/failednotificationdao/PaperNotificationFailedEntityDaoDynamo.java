package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.middleware.model.entity.PaperNotificationFailedEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class PaperNotificationFailedEntityDaoDynamo extends AbstractDynamoKeyValueStore<PaperNotificationFailedEntity> implements PaperNotificationFailedEntityDao<Key, PaperNotificationFailedEntity> {
    
    protected PaperNotificationFailedEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        super(dynamoDbEnhancedClient.table(PaperNotificationFailedEntity.TABLE_NAME, TableSchema.fromClass(PaperNotificationFailedEntity.class)));
    }

    @Override
    public Set<PaperNotificationFailedEntity> findByRecipientId(String recipientId) {
        PageIterable<PaperNotificationFailedEntity> timelineElementPages = table.query(keyEqualTo(k -> k.partitionValue(recipientId)));

        Set<PaperNotificationFailedEntity> set = new HashSet<>();
        timelineElementPages.stream().forEach(pages -> set.addAll(pages.items()));

        return set;
    }

    @Override
    public void putIfAbsent(PaperNotificationFailedEntity value) throws IdConflictException {
        String expression = "attribute_not_exists(" + PaperNotificationFailedEntity.FIELD_RECIPIENT_ID
                +") AND attribute_not_exists("+ PaperNotificationFailedEntity.FIELD_IUN +")";

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        PutItemEnhancedRequest<PaperNotificationFailedEntity> request = PutItemEnhancedRequest.builder( PaperNotificationFailedEntity.class )
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
