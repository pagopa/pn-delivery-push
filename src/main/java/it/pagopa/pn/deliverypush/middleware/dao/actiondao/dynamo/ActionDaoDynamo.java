package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;

import static it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore.ATTRIBUTE_NOT_EXISTS;
import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

@Component
@Slf4j
@ConditionalOnProperty(name = ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class ActionDaoDynamo implements ActionDao {
    private final ActionEntityDao actionEntityDao;
    private final FutureActionEntityDao futureActionEntityDao;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<ActionEntity> dynamoDbTableAction;
    private final DynamoDbTable<FutureActionEntity> dynamoDbTableFutureAction;
    private final Duration actionTtl;
    
    public ActionDaoDynamo(ActionEntityDao actionEntityDao,
                           FutureActionEntityDao futureActionEntityDao,
                           DynamoDbEnhancedClient dynamoDbEnhancedClient,
                           PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.actionEntityDao = actionEntityDao;
        this.futureActionEntityDao = futureActionEntityDao;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.dynamoDbTableAction = dynamoDbEnhancedClient.table(  pnDeliveryPushConfigs.getActionDao().getTableName(), TableSchema.fromClass(ActionEntity.class));
        this.dynamoDbTableFutureAction = dynamoDbEnhancedClient.table( pnDeliveryPushConfigs.getFutureActionDao().getTableName(), TableSchema.fromClass(FutureActionEntity.class));
        this.actionTtl = fromStringDaysToDuration(pnDeliveryPushConfigs.getActionTtlDays());
    }

    private static Duration fromStringDaysToDuration(String daysToFormat) {
        if(daysToFormat != null){
            long days = Long.parseLong(daysToFormat);
            return Duration.ofDays(days);
        }else {
            throw new PnInternalException("TTL for action cannot be null", ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

    @Override
    public void addOnlyActionIfAbsent(Action action) {
        String expression = String.format(
                "%s(%s)",
                ATTRIBUTE_NOT_EXISTS,
                ActionEntity.FIELD_ACTION_ID
        );

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        PutItemEnhancedRequest<ActionEntity> request = PutItemEnhancedRequest.builder( ActionEntity.class )
                .item(DtoToEntityActionMapper.dtoToEntity(action, actionTtl) )
                .conditionExpression( conditionExpressionPut )
                .build();
        try {
            dynamoDbTableAction.putItem(request);
        }catch (ConditionalCheckFailedException ex){
            log.warn("Exception code ConditionalCheckFailed is expected for retry, letting flow continue actionId={} ", action.getActionId(), ex);
        }
    }

    @Override
    public void addActionAndFutureActionIfAbsent(Action action, String timeSlot) {
        try {
            TransactPutItemEnhancedRequest<ActionEntity> putItemEnhancedRequest = actionEntityDao.preparePutIfAbsent(DtoToEntityActionMapper.dtoToEntity(action, actionTtl));
            TransactPutItemEnhancedRequest<FutureActionEntity> putItemEnhancedRequestFuture = futureActionEntityDao.preparePut(DtoToEntityFutureActionMapper.dtoToEntity(action,timeSlot));
            TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(dynamoDbTableAction,  putItemEnhancedRequest)
                    .addPutItem(dynamoDbTableFutureAction, putItemEnhancedRequestFuture)
                    .build();
            dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest);
        } catch (TransactionCanceledException ex){
            if (ex.hasCancellationReasons())
            {
                for (CancellationReason cr:
                     ex.cancellationReasons()) {
                    if (StringUtils.hasText(cr.code()) && cr.code().equals("ConditionalCheckFailed"))
                    {
                        log.warn("Exception code ConditionalCheckFailed is expected for retry, letting flow continue actionId={} cancellationReason is {}", action.getActionId(), cr);
                    }else if (StringUtils.hasText(cr.code()) && ! cr.code().equals("None"))
                    {
                        log.warn("TransactionCanceledException have cancellation reason but is not ConditionalCheckFailed, cancellationReason is {} actionId={}", cr, action.getActionId());
                        throw ex;
                    }
                }
            }
            else {
                log.warn("TransactionCanceledException haven't cancellation reason, actionId={} throw exception", action.getActionId(), ex);
                throw ex;
            }
        }
    }

    @Override
    public void unScheduleFutureAction(Action action, String timeSlot) {
        updateItemLogicalDeleted(action, timeSlot);
    }

    private void updateItemLogicalDeleted(Action action, String timeSlot) {
        FutureActionEntity entity = FutureActionEntity.builder()
                .timeSlot(timeSlot)
                .actionId(action.getActionId())
                .logicalDeleted(true)
                .build();

        String conditionExpression = String.format(
                "attribute_exists(%s)",
                FutureActionEntity.FIELD_TIME_SLOT
        );

        Expression condition = Expression.builder()
                .expression(conditionExpression)
                .build();

        UpdateItemEnhancedRequest<FutureActionEntity> updateItemEnhancedRequest =
                UpdateItemEnhancedRequest.builder(FutureActionEntity.class)
                        .item(entity)
                        .ignoreNulls(true)
                        .conditionExpression(condition)
                        .build();
        try {
            dynamoDbTableFutureAction.updateItem(updateItemEnhancedRequest);
        } catch (ConditionalCheckFailedException ex){
            log.warn("Exception code ConditionalCheckFailed on update future action, letting flow continue actionId={} ", action.getActionId());
        }
    }

}
