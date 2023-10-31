package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TimelineElementInternal implements Comparable<TimelineElementInternal> {
    private String iun;
    private String elementId;
    private Instant timestamp;
    private String paId;
    private List<LegalFactsIdInt> legalFactsIds;
    private TimelineElementCategoryInt category;
    private TimelineElementDetailsInt details;
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
