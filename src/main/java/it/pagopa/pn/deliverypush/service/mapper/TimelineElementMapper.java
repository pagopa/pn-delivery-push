package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV26;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV26;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV26;

public class TimelineElementMapper {
    private TimelineElementMapper(){}
    
    public static TimelineElementV26 internalToExternal(TimelineElementInternal internalDto) {
        var builder = TimelineElementV26.builder()
                .category(internalDto.getCategory() != null ? TimelineElementCategoryV26.fromValue( internalDto.getCategory().getValue() ) : null)
                .elementId(internalDto.getElementId())
                .timestamp(internalDto.getTimestamp())
                .notificationSentAt(internalDto.getNotificationSentAt())
                .ingestionTimestamp(internalDto.getIngestionTimestamp())
                .eventTimestamp(internalDto.getEventTimestamp())
                .details( SmartMapper.mapToClass(internalDto.getDetails(), TimelineElementDetailsV26.class) );
        
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
