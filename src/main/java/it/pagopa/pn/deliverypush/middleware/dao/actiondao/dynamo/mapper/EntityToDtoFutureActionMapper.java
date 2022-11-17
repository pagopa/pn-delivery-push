package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;

public class EntityToDtoFutureActionMapper {
    private EntityToDtoFutureActionMapper(){}
    
    public static Action entityToDto(FutureActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .timelineId(entity.getTimelineId())
                .details(parseDetailsFromEntity(entity.getDetails(),entity.getType()))
                .iun(entity.getIun());
        return builder.build();
    }
    
    private static ActionDetails parseDetailsFromEntity(ActionDetailsEntity entity, ActionType type) {
      return SmartMapper.mapToClass(entity, type.getDetailsJavaClass());
    }
}
