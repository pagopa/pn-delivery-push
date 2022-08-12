package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class ScheduleRefinementDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    
    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}
