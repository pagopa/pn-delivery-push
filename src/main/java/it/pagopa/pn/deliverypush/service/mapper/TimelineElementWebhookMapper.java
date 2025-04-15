package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;


public class TimelineElementWebhookMapper {
    private TimelineElementWebhookMapper(){}

    public static TimelineElementV27 internalToExternal(TimelineElementInternal internalDto) {
        // passo da TimelineElementMapper.internalToExternal(internalDto) in modo da replicare gli stessi controlli già presenti per il mapper di delivery push
        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27 timelineElement = TimelineElementMapper.internalToExternal(internalDto);
        return SmartMapper.mapToClass(timelineElement, TimelineElementV27.class);
    }
}