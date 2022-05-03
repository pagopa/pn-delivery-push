package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;

import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    void addTimelineElement(TimelineElement element);

    Optional<TimelineElement> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass);

    Set<TimelineElement> getTimeline(String iun);

    boolean isPresentTimeLineElement(String iun, int recIndex, TimelineEventId timelineEventId);

}
