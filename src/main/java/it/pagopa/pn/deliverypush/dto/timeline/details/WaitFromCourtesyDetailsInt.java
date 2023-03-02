package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class WaitFromCourtesyDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant analogWorkflowWaitingTime;

    public String toLog() {
        return String.format(
                "recIndex=%d analogWorkflowWaitingTime=%s",
                recIndex,
                analogWorkflowWaitingTime
        );
    }
}
