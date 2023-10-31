package it.pagopa.pn.deliverypush.dto.timeline.details;

import java.time.Instant;

public interface ElementTimestampTimelineElementDetails extends TimelineElementDetailsInt {

    Instant getElementTimestamp();
}
