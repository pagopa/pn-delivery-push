package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * TimelineServiceSelector is a service that selects the appropriate TimelineService implementation
 * based on the notification's sentAt timestamp or the sys date.
 */
@Primary
@Service
@AllArgsConstructor
public class TimelineServiceSelector implements TimelineService {
    private final TimelineServiceFactory timelineServiceFactory;


    @Override
    public boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService(notification.getSentAt());
        return timelineService.addTimelineElement(element, notification);
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineElement(iun, timelineId);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineElementStrongly(iun, timelineId);
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineElementDetails(iun, timelineId, timelineDetailsClass);
    }

    @Override
    public <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category, timelineDetailsClass);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementForSpecificRecipient(String iun, int recIndex, TimelineElementCategoryInt category) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, category);
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimeline(iun, confidentialInfoRequired);
    }

    @Override
    public Set<TimelineElementInternal> getTimelineStrongly(String iun, boolean confidentialInfoRequired) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineStrongly(iun, confidentialInfoRequired);
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
    }

    @Override
    public Mono<ProbableSchedulingAnalogDateResponse> getSchedulingAnalogDate(String iun, String recipientId) {
        TimelineService timelineService = timelineServiceFactory.createTimelineService();
        return timelineService.getSchedulingAnalogDate(iun, recipientId);
    }
}
