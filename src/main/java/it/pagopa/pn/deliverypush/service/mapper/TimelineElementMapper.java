package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV27;

public class TimelineElementMapper {
    private TimelineElementMapper(){}
    
    public static TimelineElementV27 internalToExternal(TimelineElementInternal internalDto) {
        var builder = TimelineElementV27.builder()
                .category(internalDto.getCategory() != null ? TimelineElementCategoryV27.fromValue( internalDto.getCategory().getValue() ) : null)
                .elementId(internalDto.getElementId())
                .timestamp(internalDto.getTimestamp())
                .notificationSentAt(internalDto.getNotificationSentAt())
                .ingestionTimestamp(internalDto.getIngestionTimestamp())
                .eventTimestamp(internalDto.getEventTimestamp())
                .details( SmartMapper.mapToClass(internalDto.getDetails(), TimelineElementDetailsV27.class) );
        
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
