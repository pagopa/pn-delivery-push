package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV23;

public class TimelineElementMapper {
    private TimelineElementMapper(){}
    
    public static TimelineElementV23 internalToExternal(TimelineElementInternal internalDto) {
        var builder = TimelineElementV23.builder()
                .category(internalDto.getCategory() != null ? TimelineElementCategoryV23.fromValue( internalDto.getCategory().getValue() ) : null)
                .elementId(internalDto.getElementId())
                .timestamp(internalDto.getTimestamp())
                .details( SmartMapper.mapToClass(internalDto.getDetails(), TimelineElementDetailsV23.class) );
        
        if(internalDto.getLegalFactsIds() != null){
            builder.legalFactsIds(
                    internalDto.getLegalFactsIds().stream()
                            .map(LegalFactIdMapper::internalToExternal)
                            .toList()
            );
        }

        return builder.build();
    }
}
