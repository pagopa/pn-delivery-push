package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PublicRegistryCallDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private DeliveryModeInt deliveryMode;
    private ContactPhaseInt contactPhase;
    private int sentAttemptMade;
    private Instant sendDate;
}
