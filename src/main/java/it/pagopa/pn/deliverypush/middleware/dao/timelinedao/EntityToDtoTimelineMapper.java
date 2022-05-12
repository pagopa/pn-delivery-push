package it.pagopa.pn.deliverypush.middleware.dao.timelinedao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
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
                .category( TimelineElementCategory.valueOf(entity.getCategory()) )
                .timestamp( entity.getTimestamp() )
                .details( parseDetailsFromJson( entity ))
                .legalFactsIds( parseLegalFactIdsFromJson( entity ) )
                .build();
    }

    private List<LegalFactsId> parseLegalFactIdsFromJson(TimelineElementEntity entity) {
        try {
            LegalFactsId[] legalFactsListEntryIds;
            legalFactsListEntryIds = objectMapper.readValue( entity.getLegalFactId(), LegalFactsId[].class );
            return legalFactsListEntryIds == null ? null : Arrays.asList( legalFactsListEntryIds );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Reading timeline detail from storage", exc );
        }
    }

    private TimelineElementDetails parseDetailsFromJson( TimelineElementEntity entity) {
        try {
            ObjectReader objectReader = this.objectMapper.readerFor( TimelineElementDetails.class );
            return objectReader.readValue( entity.getDetails() );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Reading timeline detail from storage", exc );
        }
    }
    
}
