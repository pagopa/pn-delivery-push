package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;


public class TimelineElementWebhookMapper {
    private TimelineElementWebhookMapper(){}

    public static TimelineElementV23 internalToExternal(TimelineElementInternal internalDto) {
        var builder = TimelineElementV23.builder()
                .category(internalDto.getCategory() != null ? TimelineElementCategoryV23.fromValue( internalDto.getCategory().getValue() ) : null)
                .elementId(internalDto.getElementId())
                .timestamp(internalDto.getTimestamp())
                .details( SmartMapper.mapToClass(internalDto.getDetails(), TimelineElementDetailsV23.class) );

        if(internalDto.getLegalFactsIds() != null){
            builder.legalFactsIds(
                    internalDto.getLegalFactsIds().stream()
                            .map(legalFact ->  LegalFactsId.builder()
                                        .key(legalFact.getKey())
                                        .category( legalFact.getCategory() != null ? LegalFactCategory.fromValue(legalFact.getCategory().getValue()): null)
                                        .build()
                            ).toList()
            );
        }

        return builder.build();
    }
}
