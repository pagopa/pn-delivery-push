package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;

    public TimeLineServiceImpl(TimelineDao timelineDao) {
        this.timelineDao = timelineDao;
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
    public boolean isPresentTimeLineElement(String iun, String taxId, TimelineEventId timelineEventId) {
        EventId eventId = EventId.builder()
                .iun(iun)
                .recipientId(taxId)
                .build();
        return this.timelineDao.getTimelineElement(iun, timelineEventId.buildEventId(eventId)).isPresent();
    }

}
