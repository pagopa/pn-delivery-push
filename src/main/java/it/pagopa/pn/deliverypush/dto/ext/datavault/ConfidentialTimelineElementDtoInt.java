package it.pagopa.pn.deliverypush.dto.ext.datavault;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ConfidentialTimelineElementDtoInt {
    private String timelineElementId;
    private String digitalAddress;
    private PhysicalAddressInt physicalAddress;
    private PhysicalAddressInt newPhysicalAddress;
}
