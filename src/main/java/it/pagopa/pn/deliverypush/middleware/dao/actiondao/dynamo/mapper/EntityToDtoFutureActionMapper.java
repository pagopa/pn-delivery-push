package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoFutureActionMapper {


    public Action entityToDto(FutureActionEntity entity) {
        Action.ActionBuilder builder = Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .timelineId(entity.getTimelineId())
                .iun(entity.getIun());
        return builder.build();
    }
}
