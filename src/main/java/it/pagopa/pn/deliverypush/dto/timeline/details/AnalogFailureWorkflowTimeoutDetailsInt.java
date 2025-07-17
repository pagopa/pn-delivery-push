package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AnalogFailureWorkflowTimeoutDetailsInt implements ElementTimestampTimelineElementDetails, RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private String generatedAarUrl;
    private Integer notificationCost;
    private Instant timeoutDate;

    public String toLog() {
        return String.format(
                "recIndex=%d, cost=%d timeoutDate=%s",
                recIndex,
                notificationCost,
                timeoutDate
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return timeoutDate;
    }
}

