package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TimelineDaoMock implements TimelineDao {
    private Collection<TimelineElementInternal> timelineList;

    public TimelineDaoMock() {
        timelineList = new ArrayList<>();
    }

    public void clear() {
        this.timelineList = new ArrayList<>();
    }
    
    @Override
    public void addTimelineElement(TimelineElementInternal row) {
        timelineList.add(row);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        return timelineList.stream().filter(timelineElement -> iun.equals(timelineElement.getIun())).collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        throw new UnsupportedOperationException();
    }
}
