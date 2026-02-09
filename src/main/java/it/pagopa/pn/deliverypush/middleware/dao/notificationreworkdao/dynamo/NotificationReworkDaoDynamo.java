package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.NotificationReworkDao;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksErrorEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestErrorCause;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.List;
import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@Slf4j
public class NotificationReworkDaoDynamo implements NotificationReworkDao {

    private static final String ERROR_CODE_REWORK_UPDATE_NOT_ALLOWED = "ERROR_CODE_REWORK_UPDATE_NOT_ALLOWED";
    private static final String ERROR_CODE_REWORK_NOT_FOUND = "ERROR_CODE_REWORK_NOT_FOUND";
    private final String ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM = "ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM";


    private final DynamoDbAsyncTable<NotificationReworksEntity> notificationReworkTable;

    public NotificationReworkDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        this.notificationReworkTable = dynamoDbEnhancedClient.table(cfg.getNotificationReworksDao().getTableName(), TableSchema.fromBean(NotificationReworksEntity.class));
    }


    @Override
    public Mono<NotificationReworksEntity> updateStatusToPending(String iun, String reworkId) {

        String expression = "#st = :ready AND attribute_not_exists(#rcv)";

        Expression conditionExpression = Expression.builder()
                .expression(expression)
                .expressionNames(Map.of(
                        "#st", NotificationReworksEntity.FILED_STATUS,
                        "#rcv", NotificationReworksEntity.FIELD_RECEIVED_STATUS_CODES
                ))
                .expressionValues(Map.of(
                        ":ready", AttributeValue.builder().s(ReworkRequestStatus.READY.name()).build()
                ))
                .build();

        return Mono.just(new NotificationReworksEntity())
                .flatMap(entity -> {
                    entity.setReworkId(reworkId);
                    entity.setIun(iun);
                    entity.setStatus(ReworkRequestStatus.PENDING_UPDATE);

                    UpdateItemEnhancedRequest<NotificationReworksEntity> request =
                            UpdateItemEnhancedRequest.builder(NotificationReworksEntity.class)
                                    .item(entity)
                                    .ignoreNullsMode(IgnoreNullsMode.MAPS_ONLY)
                                    .returnValues(ReturnValue.ALL_NEW)
                                    .conditionExpression(conditionExpression)
                                    .build();

                    return Mono.fromFuture(notificationReworkTable.updateItem(request))
                            .onErrorMap(ConditionalCheckFailedException.class, ex -> {
                                log.error(
                                        "Conditional check failed on NotificationReworksDaoDynamo update reworkId={} exmessage={}",
                                        entity.getReworkId(),
                                        ex.getMessage()
                                );

                                return new PnInternalException(String.format("Update not allowed for reworkId %s", entity.getReworkId()), 400, ERROR_CODE_REWORK_UPDATE_NOT_ALLOWED);
                            })
                            .onErrorMap(ResourceNotFoundException.class, ex -> {
                                log.error(
                                        "Rework entity not found on NotificationReworksDaoDynamo reworkId={} iun={} exmessage={}",
                                        entity.getReworkId(),
                                        entity.getIun(),
                                        ex.getMessage()
                                );

                                return new PnInternalException(String.format("Rework entity not found for reworkId %s", entity.getReworkId()), 404, ERROR_CODE_REWORK_NOT_FOUND);
                            });
                });
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
    public Mono<Void> updateStatusError(String iun, String reworkId, String message) {
        NotificationReworksEntity item = new NotificationReworksEntity();
        item.setIun(iun);
        item.setReworkId(reworkId);
        item.setStatus(ReworkRequestStatus.ERROR);
        NotificationReworksErrorEntity notificationReworksErrorEntity = new NotificationReworksErrorEntity();
        notificationReworksErrorEntity.setDescription(message);
        notificationReworksErrorEntity.setCause(ReworkRequestErrorCause.ERROR_INSER_ACTION);
        item.setErrors(List.of(notificationReworksErrorEntity));

        return Mono.fromFuture(
                notificationReworkTable.updateItem(
                        UpdateItemEnhancedRequest.builder(NotificationReworksEntity.class)
                                .item(item)
                                .ignoreNullsMode(IgnoreNullsMode.MAPS_ONLY)
                                .build())
        ).then();
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
    public Flux<NotificationReworksEntity> findByIun(String iun) {
        Key key = Key.builder().partitionValue(iun).build();
        QueryConditional queryByHashKey = keyEqualTo(key);

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(queryByHashKey)
                .build();

        return Flux.from(notificationReworkTable.query(queryEnhancedRequest))
                .flatMapIterable(Page::items);
    }
}

