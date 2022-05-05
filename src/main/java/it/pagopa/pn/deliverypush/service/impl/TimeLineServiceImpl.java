package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineStatusHistoryDto;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    
    public TimeLineServiceImpl(TimelineDao timelineDao, StatusUtils statusUtils) {
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
    }

    @Override
    public void addTimelineElement(TimelineElement element) {
        log.debug("addTimelineElement - IUN {} and timelineId {}", element.getIun(), element.getElementId());
        timelineDao.addTimelineElement(element);
    }

    @Override
    public Optional<TimelineElement> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN {} and timelineId {}", iun, timelineId);
        return timelineDao.getTimelineElement(iun, timelineId);
    }

    @Override
    public <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("GetTimelineElement - IUN {} and timelineId {}", iun, timelineId);

        Optional<TimelineElement> row = this.timelineDao.getTimelineElement(iun, timelineId);
        return row.map(el -> timelineDetailsClass.cast(el.getDetails()));
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        log.debug("GetTimeline - iun {} ", iun);
        return this.timelineDao.getTimeline(iun);
    }

    @Override
    public TimelineStatusHistoryDto getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory - iun {} ", iun);
        Set<TimelineElement> timelineElements = this.timelineDao.getTimeline(iun);

        List<NotificationStatusHistoryElement> statusHistory = statusUtils
                .getStatusHistory( timelineElements, numberOfRecipients, createdAt );

        return TimelineStatusHistoryDto.builder()
                .timelineElements(timelineElements)
                .statusHistory(statusHistory)
                .build();
    }
    
    @Override
    public boolean isPresentTimeLineElement(String iun, int recIndex, TimelineEventId timelineEventId) {
        EventId eventId = EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .build();
        return this.timelineDao.getTimelineElement(iun, timelineEventId.buildEventId(eventId)).isPresent();
    }

}
