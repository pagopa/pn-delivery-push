package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EntityToDtoTimelineMapper {
    private final ObjectMapper objectMapper;

    public EntityToDtoTimelineMapper(ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
    }
    
    public TimelineElementInternal entityToDto(TimelineElementEntity entity ) {
        return TimelineElementInternal.timelineInternalBuilder()
                .iun(entity.getIun())
                .elementId( entity.getTimelineElementId() )
                .category( entity.getCategory() != null ? TimelineElementCategory.valueOf(entity.getCategory().getValue()) : null )
                .timestamp( entity.getTimestamp() )
                .details( parseDetailsFromEntity( entity.getDetails() ))
                .legalFactsIds( parseLegalFactIdsFromJson( entity.getLegalFactId() ) )
                .build();
    }

    private List<LegalFactsId> parseLegalFactIdsFromJson(String legalFactsId) {
        List<LegalFactsId> legalFactsIds = null;
        
        if (legalFactsId != null){
            try {
                LegalFactsId[] legalFactsListEntryIds;
                legalFactsListEntryIds = objectMapper.readValue( legalFactsId, LegalFactsId[].class );
                legalFactsIds =  legalFactsListEntryIds == null ? null : Arrays.asList( legalFactsListEntryIds );
            } catch (JsonProcessingException exc) {
                throw new PnInternalException( "Reading timeline detail from storage", exc );
            }
        }
        
        return legalFactsIds;
    }

    private TimelineElementDetails parseDetailsFromEntity(TimelineElementDetailsEntity entity) {
        return SmartMapper.mapToClass(entity, TimelineElementDetails.class );
    }
}