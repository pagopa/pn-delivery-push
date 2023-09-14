package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class PrepareAnalogDomicileFailureDetailsInt implements RecipientRelatedTimelineElementDetails, FoundAddressRelatedTimelineElement {

    private Integer recIndex;
    private PhysicalAddressInt foundAddress;
    private String failureCause;
    private String prepareRequestId;

    public String toLog() {
        return String.format(
            "failureCause=%s recIndex=%d prepareRequestId=%s",
            failureCause,
            recIndex,
            prepareRequestId
        );
    }

}