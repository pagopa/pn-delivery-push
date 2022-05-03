package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;

import java.util.Optional;
import java.util.Set;

public interface TimelineDao {

    static final String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.timeline-dao";

    void addTimelineElement( TimelineElement row );

    Optional<TimelineElement> getTimelineElement( String iun, String timelineId );

    Set<TimelineElement> getTimeline(String iun );

    void deleteTimeline( String iun );

}
