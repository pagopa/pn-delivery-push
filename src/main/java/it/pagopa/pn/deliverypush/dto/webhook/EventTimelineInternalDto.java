package it.pagopa.pn.deliverypush.dto.webhook;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class EventTimelineInternalDto {
    private EventEntity eventEntity;
    private TimelineElementInternal timelineElementInternal;

}
