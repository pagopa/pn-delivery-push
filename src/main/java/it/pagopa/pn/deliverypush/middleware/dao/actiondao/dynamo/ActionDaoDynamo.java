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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@ConditionalOnProperty(name = ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class ActionDaoDynamo implements ActionDao {
    private final ActionEntityDao actionEntityDao;
    private final FutureActionEntityDao futureActionEntityDao;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<ActionEntity> dynamoDbTableAction;
    private final DynamoDbTable<FutureActionEntity> dynamoDbTableFutureAction;

    public ActionDaoDynamo(ActionEntityDao actionEntityDao,
                           FutureActionEntityDao futureActionEntityDao,
                           DynamoDbEnhancedClient dynamoDbEnhancedClient,
                           PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.actionEntityDao = actionEntityDao;
        this.futureActionEntityDao = futureActionEntityDao;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.dynamoDbTableAction = dynamoDbEnhancedClient.table(  pnDeliveryPushConfigs.getActionDao().getTableName(), TableSchema.fromClass(ActionEntity.class));
        this.dynamoDbTableFutureAction = dynamoDbEnhancedClient.table( pnDeliveryPushConfigs.getFutureActionDao().getTableName(), TableSchema.fromClass(FutureActionEntity.class));

    }

    @Override
    public void addAction(Action action, String timeSlot) {
        actionEntityDao.put(DtoToEntityActionMapper.dtoToEntity(action));
        futureActionEntityDao.put(DtoToEntityFutureActionMapper.dtoToEntity(action,timeSlot));
    }

    @Override
    public void addActionIfAbsent(Action action, String timeSlot) {
        try {
            TransactPutItemEnhancedRequest<ActionEntity> putItemEnhancedRequest = actionEntityDao.preparePutIfAbsent(DtoToEntityActionMapper.dtoToEntity(action));
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
                        log.warn("Exception code ConditionalCheckFailed is expected for retry, letting flow continue actionId={}", action.getActionId());
                        return;
                    }
                }
            }
            else
                throw ex;
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
