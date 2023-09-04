package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;

public class TimelineElementMapper {
    private TimelineElementMapper(){}
    
    public static TimelineElementV20 internalToExternal(TimelineElementInternal internalDto) {
        var builder = TimelineElementV20.builder()
                .category(internalDto.getCategory() != null ? TimelineElementCategoryV20.fromValue( internalDto.getCategory().getValue() ) : null)
                .elementId(internalDto.getElementId())
                .timestamp(internalDto.getTimestamp())
                .details( SmartMapper.mapToClass(internalDto.getDetails(), TimelineElementDetailsV20.class) );
        
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
