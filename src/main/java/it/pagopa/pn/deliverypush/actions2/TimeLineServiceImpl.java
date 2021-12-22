package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
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
        Optional<TimelineElement> row;
        row = this.timelineDao.getTimelineElement(iun, timelineId);
        return row.map(el -> timelineDetailsClass.cast(el.getDetails()));
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        //TODO Implementare metodo
        throw new UnsupportedOperationException();
    }
}
