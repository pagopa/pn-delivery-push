package it.pagopa.pn.deliverypush.middleware.dao.timelinedao;

import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineCounterEntity;

public interface TimelineCounterEntityDao {


    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.timeline-counter-dao";

    TimelineCounterEntity getCounter(String timelineElementId);
}
