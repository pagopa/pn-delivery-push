package it.pagopa.pn.deliverypush.action2.it.testbean;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TimelineDaoTest implements TimelineDao {
    List<TimelineElement> timelineList;

    public TimelineDaoTest() {
        timelineList = new ArrayList<>();
    }

    @Override
    public void addTimelineElement(TimelineElement row) {
        timelineList.add(row);
    }

    @Override
    public Optional<TimelineElement> getTimelineElement(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineElement.getElementId().equals(timelineId) && timelineElement.getIun().equals(iun)).findFirst();
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        return timelineList.stream().filter(timelineElement -> timelineElement.getIun().equals(iun)).collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        throw new UnsupportedOperationException();
    }
}
