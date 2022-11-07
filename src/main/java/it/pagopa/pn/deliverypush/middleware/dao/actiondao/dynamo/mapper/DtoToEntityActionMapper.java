package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

public class DtoToEntityActionMapper {
    private DtoToEntityActionMapper(){}
    
    public static ActionEntity dtoToEntity(Action dto) {
        ActionEntity.ActionEntityBuilder builder =  ActionEntity.builder()
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .timeslot(dto.getTimeslot())
                .timelineId(dto.getTimelineId())
                .iun(dto.getIun())
                .details(dto.getDetails());
        
        return builder.build();
    }

}
