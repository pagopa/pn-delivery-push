package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SimpleRegisteredLetterDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private String foreignState;
    private Integer numberOfPages;
}
