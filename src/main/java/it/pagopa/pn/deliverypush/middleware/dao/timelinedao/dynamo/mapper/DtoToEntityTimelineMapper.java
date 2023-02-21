package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.*;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoToEntityTimelineMapper {
    
    public TimelineElementEntity dtoToEntity(TimelineElementInternal dto) {
        return TimelineElementEntity.builder()
                .iun( dto.getIun() )
                .timelineElementId( dto.getElementId() )
                .paId( dto.getPaId() )
                .category( TimelineElementCategoryEntity.valueOf( dto.getCategory().getValue() ) )
                .timestamp( dto.getTimestamp() )
                .details( dtoToDetailsEntity( dto.getDetails() ) )
                .legalFactIds( convertLegalFactsToEntity( dto.getLegalFactsIds() ) )
                .statusInfo(dtoToStatusInfoEntity(dto.getStatusInfo()))
                .notificationSentAt(dto.getNotificationSentAt())
                .build();
    }

    private List<LegalFactsIdEntity> convertLegalFactsToEntity(List<LegalFactsIdInt>  dto ) {
        List<LegalFactsIdEntity> legalFactsIds = null;

        if (dto != null){
            legalFactsIds = dto.stream().map( this::mapOneLegalFact ).toList();
        }

        return legalFactsIds;
    }

    private LegalFactsIdEntity mapOneLegalFact(LegalFactsIdInt legalFactsId) {
        LegalFactsIdEntity entity = new LegalFactsIdEntity();
        entity.setKey( legalFactsId.getKey() );
        entity.setCategory(LegalFactCategoryEntity.valueOf( legalFactsId.getCategory().getValue()));
        return entity;
    }
    
    private TimelineElementDetailsEntity dtoToDetailsEntity(TimelineElementDetailsInt details) {
        return SmartMapper.mapToClass(details, TimelineElementDetailsEntity.class );
    }

    private StatusInfoEntity dtoToStatusInfoEntity(StatusInfoInternal statusInfoInternal) {
        if(statusInfoInternal == null) return null;
        return StatusInfoEntity.builder()
                .statusChangeTimestamp(statusInfoInternal.getStatusChangeTimestamp())
                .statusChanged(statusInfoInternal.isStatusChanged())
                .actual(statusInfoInternal.getActual())
                .build();
    }
}
