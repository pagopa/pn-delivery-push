package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalSuccessWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
}
