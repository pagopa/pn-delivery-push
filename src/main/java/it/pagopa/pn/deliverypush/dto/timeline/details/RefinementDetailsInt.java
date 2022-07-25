package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RefinementDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Integer notificationCost;
}
