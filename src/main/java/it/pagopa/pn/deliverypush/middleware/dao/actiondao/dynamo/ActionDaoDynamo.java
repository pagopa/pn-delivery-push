package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.FutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore.ATTRIBUTE_NOT_EXISTS;

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
        this.actionTtl = pnDeliveryPushConfigs.getActionTtl();
    }

    @Override
    public void addAction(Action action, String timeSlot) {
        actionEntityDao.put(DtoToEntityActionMapper.dtoToEntity(action, actionTtl));
        futureActionEntityDao.put(DtoToEntityFutureActionMapper.dtoToEntity(action,timeSlot));
    }

    @Override
    public void addOnlyAction(Action action) {
        actionEntityDao.put(DtoToEntityActionMapper.dtoToEntity(action, actionTtl));
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
    public Optional<Action> getActionById(String actionId) {
        Key keyToSearch = Key.builder()
                .partitionValue(actionId)
                .build();
        
        return actionEntityDao.get(keyToSearch)
                .map(EntityToDtoActionMapper::entityToDto);
    }

    @Override
    public List<Action> findActionsByTimeSlot(String timeSlot) {

        Set<FutureActionEntity> entities = futureActionEntityDao.findByTimeSlot(timeSlot);

        return entities.stream()
                .map(EntityToDtoFutureActionMapper::entityToDto)
                .toList();
    }

    @Override
    public void unSchedule(Action action, String timeSlot) {
        Key keyToDelete = Key.builder()
                .partitionValue(timeSlot)
                .sortValue(action.getActionId())
                .build();
        
        futureActionEntityDao.delete(keyToDelete);
    }
}
