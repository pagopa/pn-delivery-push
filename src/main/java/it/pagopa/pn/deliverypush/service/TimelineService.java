package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;

import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    void addTimelineElement(TimelineElement element);

    Optional<TimelineElement> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass);

    Set<TimelineElement> getTimeline(String iun);

    boolean isPresentTimeLineElement(String iun, String taxId, TimelineEventId timelineEventId);

}
