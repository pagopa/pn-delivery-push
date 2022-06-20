package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendAnalogDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private ServiceLevelInt serviceLevel;
    private Integer sentAttemptMade;
    private boolean investigation;
    private Integer numberOfPages;
}
