package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class TimelineElementInternal implements Comparable<TimelineElementInternal> {
    private final String iun;
    private final String elementId;
    private final Instant timestamp;
    private final String paId;
    private final List<LegalFactsIdInt> legalFactsIds;
    private final TimelineElementCategoryInt category;
    private final TimelineElementDetailsInt details;
    private StatusInfoInternal statusInfo;
    private Instant notificationSentAt;

    @Override
    public int compareTo(@NotNull TimelineElementInternal o) {
        int order = this.timestamp.compareTo(o.getTimestamp());
        if (order == 0)
            order = this.category.getPriority() - o.getCategory().getPriority();
        return order;
    }
}
