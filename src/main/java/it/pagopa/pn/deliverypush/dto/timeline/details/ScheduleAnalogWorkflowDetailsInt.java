package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class ScheduleAnalogWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
}
