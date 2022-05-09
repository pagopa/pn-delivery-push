package it.pagopa.pn.deliverypush.middleware.timelinedao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.util.TimelineDetailMap;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EntityToDtoTimelineMapper {

    private final ObjectMapper objectMapper;
    private final Map<TimelineElementCategory, ObjectReader> objectReaders;

    public EntityToDtoTimelineMapper(ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
        this.objectReaders = new ConcurrentHashMap<>();
    }

    public TimelineElementInternal entityToDto(TimelineElementEntity entity ) {
        return new TimelineElementInternal(
                TimelineElement.builder()
                .elementId( entity.getTimelineElementId() )
                .category( TimelineElementCategory.valueOf(entity.getCategory()) )
                .timestamp( DateUtils.convertInstantToDate(entity.getTimestamp()) )
                .details( parseDetailsFromJson( entity ))
                .legalFactsIds( parseLegalFactIdsFromJson( entity ) )
                .build(),
                entity.getIun());
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

            TimelineElementCategory category = TimelineElementCategory.valueOf(entity.getCategory());
            ObjectReader objectReader = getObjectReader( category );
            return objectReader.readValue( entity.getDetails() );

        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Reading timeline detail from storage", exc );
        }
    }

    private ObjectReader getObjectReader( TimelineElementCategory timelineElementCategory ) {
        return this.objectReaders.computeIfAbsent(
                timelineElementCategory,
                // - generate reader of needed: objectReader is thread safe, object mapper don't
                category -> {
                    synchronized ( this.objectMapper ) {
                        return this.objectMapper.readerFor( TimelineDetailMap.getDetailJavaClass(category) );
                    }
                }
            );
    }

}
