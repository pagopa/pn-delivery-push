package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TimelineDaoMock implements TimelineDao {
    private Collection<TimelineElement> timelineList;

    public TimelineDaoMock() {
        timelineList = new ArrayList<>();
    }

    public void clear() {
        this.timelineList = new ArrayList<>();
    }
    
    @Override
    public void addTimelineElement(TimelineElement row) {
        timelineList.add(row);
    }

    @Override
    public Optional<TimelineElement> getTimelineElement(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        return timelineList.stream().filter(timelineElement -> iun.equals(timelineElement.getIun())).collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        throw new UnsupportedOperationException();
    }
}
