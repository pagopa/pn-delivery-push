package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class ScheduleAnalogWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant schedulingDate;

    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}
