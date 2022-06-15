package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AnalogFailureWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
}
