package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
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
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnProperty(name = ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class ActionDaoDynamo implements ActionDao {
    private final ActionEntityDao actionEntityDao;
    private final FutureActionEntityDao futureActionEntityDao;
    private final DtoToEntityActionMapper dtoToEntityActionMapper;
    private final DtoToEntityFutureActionMapper dtoToEntityFutureActionMapper;
    private final EntityToDtoActionMapper entityToDtoActionMapper;
    private final EntityToDtoFutureActionMapper entityToDtoFutureActionMapper;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<ActionEntity> dynamoDbTableAction;
    private final DynamoDbTable<FutureActionEntity> dynamoDbTableFutureAction;


    public ActionDaoDynamo(ActionEntityDao actionEntityDao, FutureActionEntityDao futureActionEntityDao, DtoToEntityActionMapper dtoToEntityActionMapper,
                           DtoToEntityFutureActionMapper dtoToEntityFutureActionMapper, EntityToDtoActionMapper entityToDtoActionMapper, EntityToDtoFutureActionMapper entityToDtoFutureActionMapper, DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.actionEntityDao = actionEntityDao;
        this.futureActionEntityDao = futureActionEntityDao;
        this.dtoToEntityActionMapper = dtoToEntityActionMapper;
        this.dtoToEntityFutureActionMapper = dtoToEntityFutureActionMapper;
        this.entityToDtoActionMapper = entityToDtoActionMapper;
        this.entityToDtoFutureActionMapper = entityToDtoFutureActionMapper;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;

        this.dynamoDbTableAction = dynamoDbEnhancedClient.table(  pnDeliveryPushConfigs.getActionDao().getTableName(), TableSchema.fromClass(ActionEntity.class));
        this.dynamoDbTableFutureAction = dynamoDbEnhancedClient.table( pnDeliveryPushConfigs.getFutureActionDao().getTableName(), TableSchema.fromClass(FutureActionEntity.class));

    }

    @Override
    public void addAction(Action action, String timeSlot) {
        actionEntityDao.put(dtoToEntityActionMapper.dtoToEntity(action));
        futureActionEntityDao.put(dtoToEntityFutureActionMapper.dtoToEntity(action,timeSlot));
    }

    @Override
    public void addActionIfAbsent(Action action, String timeSlot) {
        try {
            PutItemEnhancedRequest<ActionEntity> putItemEnhancedRequest = actionEntityDao.preparePutIfAbsent(dtoToEntityActionMapper.dtoToEntity(action));
            PutItemEnhancedRequest<FutureActionEntity> putItemEnhancedRequestFuture = futureActionEntityDao.preparePut(dtoToEntityFutureActionMapper.dtoToEntity(action,timeSlot));
            TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(dynamoDbTableAction,  putItemEnhancedRequest)
                    .addPutItem(dynamoDbTableFutureAction, putItemEnhancedRequestFuture)
                    .build();
            dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest);
        } catch (ConditionalCheckFailedException ex){
            log.warn("Conditional check exception on ActionDaoDynamo addActionIfAbsent", ex);
        }
    }
    
    @Override
    public Optional<Action> getActionById(String actionId) {
        Key keyToSearch = Key.builder()
                .partitionValue(actionId)
                .build();
        
        return actionEntityDao.get(keyToSearch)
                .map(entityToDtoActionMapper::entityToDto);
    }

    @Override
    public List<Action> findActionsByTimeSlot(String timeSlot) {

        Set<FutureActionEntity> entities = futureActionEntityDao.findByTimeSlot(timeSlot);

        return entities.stream()
                .map(entityToDtoFutureActionMapper::entityToDto)
                .collect(Collectors.toList());
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
