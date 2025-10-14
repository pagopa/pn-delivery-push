package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.NotificationReworkDao;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@Slf4j
public class NotificationReworkDaoDynamo implements NotificationReworkDao {

    private final String ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM = "ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM";

    private final DynamoDbAsyncTable<NotificationReworksEntity> notificationReworkTable;

    public NotificationReworkDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        this.notificationReworkTable = dynamoDbEnhancedClient.table(cfg.getNotificationReworksDao().getTableName(), TableSchema.fromBean(NotificationReworksEntity.class));
    }

    public Mono<NotificationReworksEntity> findLatestByIun(String iun) {
        QueryConditional queryByHashKey = QueryConditional.keyEqualTo(Key.builder().partitionValue(iun).build());

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryByHashKey)
                .scanIndexForward(false)
                .limit(1)
                .build();

        return Mono.from(notificationReworkTable.query(request))
                .flatMap(page -> Mono.justOrEmpty(page.items().stream().findFirst()));
    }

    @Override
    public Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId) {
        Key hashKey = Key.builder().partitionValue(iun).sortValue(reworkId).build();
        return Mono.fromFuture(notificationReworkTable.getItem(hashKey));
    }

    @Override
    public Mono<NotificationReworksEntity> putIfAbsent(NotificationReworksEntity entity) {
        String expression = String.format("attribute_not_exists(%s) AND attribute_not_exists(%s)",
                NotificationReworksEntity.FIELD_REWORK_ID,
                NotificationReworksEntity.FIELD_IUN);

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        PutItemEnhancedRequest<NotificationReworksEntity> request = PutItemEnhancedRequest.builder(NotificationReworksEntity.class)
                .item(entity)
                .conditionExpression(conditionExpressionPut)
                .build();

        return Mono.fromFuture(notificationReworkTable.putItem(request))
                .onErrorMap(ConditionalCheckFailedException.class, ex -> {
                    log.error("Conditional check exception on NotificationReworksDaoDynamo putIfAbsent reworkId={} exmessage={}", entity.getReworkId(), ex.getMessage());
                    return new PnConflictException(ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM, String.format("RequestId %s already exists", entity.getReworkId()));
                })
                .thenReturn(entity);

    }

    @Override
    public Mono<Page<NotificationReworksEntity>> findByIun(String iun, Map<String, AttributeValue> lastEvaluateKey, int limit) {
        Key key = Key.builder().partitionValue(iun).build();
        QueryConditional queryByHashKey = keyEqualTo(key);

        QueryEnhancedRequest.Builder queryEnhancedRequest = QueryEnhancedRequest
                .builder()
                .limit(limit)
                .queryConditional(queryByHashKey);

        if (!CollectionUtils.isEmpty(lastEvaluateKey)) {
            queryEnhancedRequest.exclusiveStartKey(lastEvaluateKey);
        }

        return Mono.from(notificationReworkTable.query(queryEnhancedRequest.build()));
    }
}

