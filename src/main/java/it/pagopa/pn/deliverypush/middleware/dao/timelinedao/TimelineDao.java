package it.pagopa.pn.deliverypush.middleware.dao.timelinedao;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;

import java.util.Optional;
import java.util.Set;

public interface TimelineDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.timeline-dao";

    void addTimelineElement(TimelineElementInternal row);

    Optional<TimelineElementInternal> getTimelineElement( String iun, String timelineId );

    Set<TimelineElementInternal> getTimeline(String iun );

    Set<TimelineElementInternal> getTimelineFilteredByElementId(String iun , String timelineId);

    void deleteTimeline( String iun );

}
