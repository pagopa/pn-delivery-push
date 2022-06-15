package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactsIdEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityToDtoTimelineMapper {
    private final ObjectMapper objectMapper;

    public EntityToDtoTimelineMapper(ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
    }
    
    public TimelineElementInternal entityToDto(TimelineElementEntity entity ) {
        return TimelineElementInternal.builder()
                .iun(entity.getIun())
                .elementId( entity.getTimelineElementId() )
                .category( entity.getCategory() != null ? TimelineElementCategory.valueOf(entity.getCategory().getValue()) : null )
                .timestamp( entity.getTimestamp() )
                .details( parseDetailsFromEntity( entity.getDetails() ))
                .legalFactsIds( convertLegalFactsFromEntity( entity.getLegalFactIds() ) )
                .build();
    }

    private List<LegalFactsIdInt> convertLegalFactsFromEntity(List<LegalFactsIdEntity>  entity ) {
        List<LegalFactsIdInt> legalFactsIds = null;
        
        if (entity != null){
            legalFactsIds = entity.stream().map( this::mapOneLegalFact ).collect(Collectors.toList());
        }
        
        return legalFactsIds;
    }

    private LegalFactsIdInt mapOneLegalFact(LegalFactsIdEntity legalFactsIdEntity) {
        String legalFactCategoryName = legalFactsIdEntity.getCategory().getValue();
        return  new LegalFactsIdInt()
                .key(legalFactsIdEntity.getKey())
                .category( LegalFactCategoryInt.valueOf( legalFactCategoryName ) );
    }

    private TimelineElementDetails parseDetailsFromEntity(TimelineElementDetailsEntity entity) {
        return SmartMapper.mapToClass(entity, TimelineElementDetails.class );
    }
}