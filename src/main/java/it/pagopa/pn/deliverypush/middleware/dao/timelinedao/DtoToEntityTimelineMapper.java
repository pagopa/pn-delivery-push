package it.pagopa.pn.deliverypush.middleware.dao.timelinedao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityTimelineMapper {

    private final ObjectMapper objectMapper;

    public DtoToEntityTimelineMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TimelineElementEntity dtoToEntity(TimelineElementInternal dto) {
        return TimelineElementEntity.builder()
                .iun( dto.getIun() )
                .timelineElementId( dto.getElementId() )
                .category( dto.getCategory().getValue())
                .timestamp( dto.getTimestamp() )
                .details( detailsToJsonString( dto ) )
                .legalFactId( legalFactIdsToJsonString ( dto ) )
                .build();
    }

    private String legalFactIdsToJsonString(TimelineElementInternal dto) {
        try {
            return objectMapper.writeValueAsString( dto.getLegalFactsIds() );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Writing timeline detail to storage", exc );
        }
    }

    private String detailsToJsonString( TimelineElementInternal dto) {
        try {
            ObjectWriter objWriter = this.objectMapper.writerFor( TimelineElementDetails.class );
            TimelineElementDetails details = dto.getDetails();
            return objWriter.writeValueAsString( details );

        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Writing timeline detail to storage", exc );
        }
    }
}
