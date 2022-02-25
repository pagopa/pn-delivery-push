package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.service.LegalFactService;

import java.util.List;
import java.util.Set;

public class LegalFactServiceImpl implements LegalFactService {

    private TimelineDao timelineDao;

    public LegalFactServiceImpl(TimelineDao timelineDao) {  this.timelineDao = timelineDao; }
    @Override
    public List<LegalFactsListEntry> getLegalFacts(String iun) {
        Set<TimelineElement> timelineElements = timelineDao.getTimeline( iun );
        for ( TimelineElement timelineElement : timelineElements ) {
        }
        return null;
    }
}
