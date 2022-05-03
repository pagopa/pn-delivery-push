package it.pagopa.pn.deliverypush.middleware.timelinedao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DtoToEntityTimelineMapper {

    private final ObjectMapper objectMapper;
    private final Map<TimelineElementCategory, ObjectWriter> objectWriters;

    public DtoToEntityTimelineMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectWriters = new ConcurrentHashMap<>();
    }

    public TimelineElementEntity dtoToEntity(TimelineElement dto) {
        return TimelineElementEntity.builder()
                .iun( dto.getIun() )
                .timelineElementId( dto.getElementId() )
                .category( dto.getCategory() )
                .timestamp( dto.getTimestamp() )
                .details( detailsToJsonString( dto ) )
                .legalFactId( legalFactIdsToJsonString ( dto ) )
                .build();
    }

    private String legalFactIdsToJsonString(TimelineElement dto) {
        try {
            return objectMapper.writeValueAsString( dto.getLegalFactsIds() );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Writing timeline detail to storage", exc );
        }
    }


    private String detailsToJsonString( TimelineElement dto) {
        try {
            TimelineElementCategory category = dto.getCategory();
            ObjectWriter objWriter = getObjectWriter( category );
            TimelineElementDetails details = dto.getDetails();
            return objWriter.writeValueAsString( details );

        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Writing timeline detail to storage", exc );
        }
    }

    private ObjectWriter getObjectWriter( TimelineElementCategory timelineElementCategory ) {
        return this.objectWriters.computeIfAbsent(
                timelineElementCategory,
                // - generate reader of needed: objectWriter is thread safe, objectMapper isn't
                category -> {
                    synchronized ( this.objectMapper ) {
                        return this.objectMapper.writerFor( category.getDetailsJavaClass() );
                    }
                }
            );
    }

}
