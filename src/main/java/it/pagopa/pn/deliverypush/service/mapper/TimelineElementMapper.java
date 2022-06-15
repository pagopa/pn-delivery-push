package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;

import java.util.stream.Collectors;

public class TimelineElementMapper {
    private TimelineElementMapper(){};
    
    public static TimelineElement internalToExternal(TimelineElementInternal internalDto) {
        TimelineElement.TimelineElementBuilder builder = TimelineElement.builder()
                .category(internalDto.getCategory() != null ? TimelineElementCategory.fromValue( internalDto.getCategory().getValue() ) : null)
                .elementId(internalDto.getElementId())
                .timestamp(internalDto.getTimestamp())
                .details( SmartMapper.mapToClass(internalDto.getDetails(), TimelineElementDetails.class) );
        
        if(internalDto.getLegalFactsIds() != null){
            builder.legalFactsIds(
                    internalDto.getLegalFactsIds().stream()
                            .map(LegalFactIdMapper::internalToExternal)
                            .collect(Collectors.toList())
            );
        }

        return builder.build();
    }
}
