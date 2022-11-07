package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionBase;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionBase.ActionBaseBuilder;

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
                .details(entity.getDetails());
        return builder.build();
        
    }

}
