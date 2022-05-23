package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    void addTimelineElement(TimelineElementInternal element);

    Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass);

    Set<TimelineElementInternal> getTimeline(String iun);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt);

    boolean isPresentTimeLineElement(String iun, Integer recIndex, TimelineEventId timelineEventId);

}
