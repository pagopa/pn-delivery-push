package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineStatusHistoryDto;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    void addTimelineElement(TimelineElement element);

    Optional<TimelineElement> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass);

    Set<TimelineElement> getTimeline(String iun);

    TimelineStatusHistoryDto getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt);

    boolean isPresentTimeLineElement(String iun, int recIndex, TimelineEventId timelineEventId);

}
