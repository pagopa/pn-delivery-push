package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;

import java.time.Instant;
import java.util.List;

public class test extends TimelineElementInternal{
    test(String iun, String elementId, Instant timestamp, String paId, List<LegalFactsIdInt> legalFactsIds, TimelineElementCategoryInt category, TimelineElementDetailsInt details) {
        super(iun, elementId, timestamp, paId, legalFactsIds, category, details);
    }
}
