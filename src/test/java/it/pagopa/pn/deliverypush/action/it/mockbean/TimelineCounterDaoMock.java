package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineCounterEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class TimelineCounterDaoMock implements TimelineCounterEntityDao {

    final HashMap<String, Long> counter = new HashMap<String, Long>();

    public TimelineCounterDaoMock() {

    }

    public void clear() {
        this.counter.clear();
    }

    @Override
    public TimelineCounterEntity getCounter(String timelineElementId) {
        Long v = 0L;
        synchronized (counter){
            if (counter.containsKey(timelineElementId))
            {
                v = counter.get(timelineElementId);
            }
            v = v+1;
            counter.put(timelineElementId, v);
        }

        TimelineCounterEntity t = new TimelineCounterEntity();
        t.setTimelineElementId(timelineElementId);
        t.setCounter(v);
        return t;
    }
}
