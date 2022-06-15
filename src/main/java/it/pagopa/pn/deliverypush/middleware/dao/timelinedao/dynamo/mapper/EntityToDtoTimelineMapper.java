package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
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
        TimelineElementCategoryInt category = entity.getCategory() != null ? TimelineElementCategoryInt.valueOf(entity.getCategory().getValue()) : null;

        return TimelineElementInternal.builder()
                .iun(entity.getIun())
                .elementId( entity.getTimelineElementId() )
                .category( category )
                .timestamp( entity.getTimestamp() )
                .details( parseDetailsFromEntity( entity.getDetails(), category) )
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
        return LegalFactsIdInt.builder()
                .key(legalFactsIdEntity.getKey())
                .category( LegalFactCategoryInt.valueOf( legalFactCategoryName ) )
                .build();
    }

    private TimelineElementDetailsInt parseDetailsFromEntity(TimelineElementDetailsEntity entity, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(entity, category.getDetailsJavaClass() );
    }
}