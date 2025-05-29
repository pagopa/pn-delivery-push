package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoActionMapper;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.util.Optional;

import static it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore.ATTRIBUTE_NOT_EXISTS;
import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

@Component
@Slf4j
@ConditionalOnProperty(name = ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class ActionDaoDynamo implements ActionDao {
    private final ActionEntityDao actionEntityDao;
    private final DynamoDbTable<ActionEntity> dynamoDbTableAction;
    private final DynamoDbTable<FutureActionEntity> dynamoDbTableFutureAction;
    private final Duration actionTtl;

    public ActionDaoDynamo(
            ActionEntityDao actionEntityDao,
            DynamoDbEnhancedClient dynamoDbEnhancedClient,
            PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.actionEntityDao = actionEntityDao;
        this.dynamoDbTableAction = dynamoDbEnhancedClient.table(pnDeliveryPushConfigs.getActionDao().getTableName(), TableSchema.fromClass(ActionEntity.class));
        this.dynamoDbTableFutureAction = dynamoDbEnhancedClient.table(pnDeliveryPushConfigs.getFutureActionDao().getTableName(), TableSchema.fromClass(FutureActionEntity.class));
        this.actionTtl = fromStringDaysToDuration(pnDeliveryPushConfigs.getActionTtlDays());
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
    public void unScheduleFutureAction(String actionId) {
        getActionById(actionId)
                .ifPresentOrElse(action -> updateItemLogicalDeleted(action, action.getTimeslot()),
                        () -> log.info("Action with id {} not found, cannot update logical deleted", actionId));
    }

    private Optional<Action> getActionById(String actionId) {
        Key keyToSearch = Key.builder()
                .partitionValue(actionId)
                .build();

        return actionEntityDao.get(keyToSearch)
                .map(EntityToDtoActionMapper::entityToDto);
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
        } catch (ConditionalCheckFailedException ex) {
            log.info("Exception code ConditionalCheckFailed on update future action, letting flow continue actionId={} ", action.getActionId());
        }
    }

    private static Duration fromStringDaysToDuration(String daysToFormat) {
        if (daysToFormat != null) {
            long days = Long.parseLong(daysToFormat);
            return Duration.ofDays(days);
        } else {
            throw new PnInternalException("TTL for action cannot be null", ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

}
