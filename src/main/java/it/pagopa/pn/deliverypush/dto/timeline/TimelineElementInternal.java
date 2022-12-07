package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class TimelineElementInternal {
    private final String iun;
    private final String elementId;
    private final Instant timestamp;
    private final String paId;
    private final List<LegalFactsIdInt> legalFactsIds;
    private final TimelineElementCategoryInt category;
    private final TimelineElementDetailsInt details;
    private StatusInfoInternal statusInfo;
    private Instant notificationSentAt;
}
