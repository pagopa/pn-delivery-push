package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;

import java.util.Optional;
import java.util.Set;

public interface TimelineService {
    void addTimelineElement(TimelineElement element);

    Optional<TimelineElement> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass);

    Set<TimelineElement> getTimeline(String iun);
}
