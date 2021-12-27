package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private TimelineDao timelineDao;

    @Override
    public void addTimelineElement(TimelineElement element) {

    }

    @Override
    public Optional<TimelineElement> getTimelineElement(String iun, String timelineId) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("Start getTimelineElement for iun {} timelineId {}", iun, timelineId);

        Optional<TimelineElement> row = this.timelineDao.getTimelineElement(iun, timelineId);
        return row.map(el -> timelineDetailsClass.cast(el.getDetails()));
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        log.debug("Start getTimeline for iun {} ", iun);
        return this.timelineDao.getTimeline(iun);
    }
}
