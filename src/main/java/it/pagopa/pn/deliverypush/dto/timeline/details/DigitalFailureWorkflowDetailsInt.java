package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalFailureWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
}
