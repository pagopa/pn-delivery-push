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
public class ProbableDateAnalogWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant schedulingAnalogDate;

    public String toLog() {
       return this.toString();
    }
}
