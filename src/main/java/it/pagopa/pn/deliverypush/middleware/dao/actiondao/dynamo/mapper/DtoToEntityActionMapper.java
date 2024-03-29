package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DtoToEntityActionMapper {
    private DtoToEntityActionMapper(){}
    
    public static ActionEntity dtoToEntity(Action dto, Duration actionTtl) {
        
        ActionEntity.ActionEntityBuilder builder =  ActionEntity.builder()
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .timeslot(dto.getTimeslot())
                .timelineId(dto.getTimelineId())
                .iun(dto.getIun())
                .details(dtoToDetailsEntity(dto.getDetails()));

        if (!actionTtl.isZero())
            builder.ttl(LocalDateTime.now().plus(actionTtl).atZone(ZoneId.systemDefault()).toEpochSecond());

        return builder.build();
    }
    
    private static ActionDetailsEntity dtoToDetailsEntity(ActionDetails details) {
      return SmartMapper.mapToClass(details, ActionDetailsEntity.class );
    }
}
