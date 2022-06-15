package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PublicRegistryResponseDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private PhysicalAddressInt physicalAddress;
}
