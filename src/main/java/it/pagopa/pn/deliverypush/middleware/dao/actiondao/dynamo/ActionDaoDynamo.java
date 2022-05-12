package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.FutureActionEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.DtoToEntityFutureActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoActionMapper;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper.EntityToDtoFutureActionMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class ActionDaoDynamo implements ActionDao {
    private final ActionEntityDao actionEntityDao;
    private final FutureActionEntityDao futureActionEntityDao;
    private final DtoToEntityActionMapper dtoToEntityActionMapper;
    private final DtoToEntityFutureActionMapper dtoToEntityFutureActionMapper;
    private final EntityToDtoActionMapper entityToDtoActionMapper;
    private final EntityToDtoFutureActionMapper entityToDtoFutureActionMapper;
    
    public ActionDaoDynamo(ActionEntityDao actionEntityDao, FutureActionEntityDao futureActionEntityDao, DtoToEntityActionMapper dtoToEntityActionMapper,
                           DtoToEntityFutureActionMapper dtoToEntityFutureActionMapper, EntityToDtoActionMapper entityToDtoActionMapper, EntityToDtoFutureActionMapper entityToDtoFutureActionMapper) {
        this.actionEntityDao = actionEntityDao;
        this.futureActionEntityDao = futureActionEntityDao;
        this.dtoToEntityActionMapper = dtoToEntityActionMapper;
        this.dtoToEntityFutureActionMapper = dtoToEntityFutureActionMapper;
        this.entityToDtoActionMapper = entityToDtoActionMapper;
        this.entityToDtoFutureActionMapper = entityToDtoFutureActionMapper;
    }

    @Override
    public void addAction(Action action, String timeSlot) {
        actionEntityDao.put(dtoToEntityActionMapper.dtoToEntity(action));
        futureActionEntityDao.put(dtoToEntityFutureActionMapper.dtoToEntity(action,timeSlot));
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
