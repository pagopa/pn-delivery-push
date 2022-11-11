package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;

public class EntityToDtoActionMapper {
    private EntityToDtoActionMapper(){}
    
    public static Action entityToDto(ActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .timelineId(entity.getTimelineId())
                .timeslot(entity.getTimeslot())
                .iun(entity.getIun())
                .details(parseDetailsFromEntity(entity.getDetails(),entity.getType()));
        return builder.build();
        
    }

    private static ActionDetails parseDetailsFromEntity(ActionDetailsEntity entity, ActionType type) {
      return SmartMapper.mapToClass(entity, type.getDetailsJavaClass());
    }
}
