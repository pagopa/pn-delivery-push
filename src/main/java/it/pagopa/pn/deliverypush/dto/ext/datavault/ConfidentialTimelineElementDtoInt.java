package it.pagopa.pn.deliverypush.dto.ext.datavault;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddressInt;
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