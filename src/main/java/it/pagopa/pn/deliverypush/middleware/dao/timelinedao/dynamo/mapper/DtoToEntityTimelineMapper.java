package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.LegalFactsIdEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementCategoryEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
                .category( TimelineElementCategoryEntity.valueOf(dto.getCategory().getValue()) )
                .timestamp( dto.getTimestamp() )
                .details( dtoToDetailsEntity( dto.getDetails() ) )
                .legalFactId( legalFactsDtoToString( dto.getLegalFactsIds() ) )
                .build();
    }

    private String legalFactsDtoToString(List<LegalFactsId> legalFactsIds) {
        String legalFacts = null;
                
        if(legalFactsIds != null){
            List<LegalFactsIdEntity> listLegalFactsEntity =  legalFactsIds.stream().map(
                    legalFactsId -> SmartMapper.mapToClass(legalFactsId, LegalFactsIdEntity.class )
            ).collect(Collectors.toList());
            legalFacts = legalFactIdsToJsonString(listLegalFactsEntity);
        }
        
        return legalFacts;
    }

    private String legalFactIdsToJsonString(List<LegalFactsIdEntity> listLegalFactsEntity) {
        try {
            return objectMapper.writeValueAsString( listLegalFactsEntity );
        } catch (JsonProcessingException exc) {
            throw new PnInternalException( "Writing timeline detail to storage", exc );
        }
    }

    private TimelineElementDetailsEntity dtoToDetailsEntity(TimelineElementDetails details) {
        return SmartMapper.mapToClass(details, TimelineElementDetailsEntity.class );
    }
}
