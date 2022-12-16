package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SimpleRegisteredLetterDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private String foreignState;
    private Integer analogCost;
    private String productType;

    public String toLog() {
        return String.format(
                "recIndex=%d physicalAddress=%s analogCost=%d productType=%s",
                recIndex,
                AuditLogUtils.SENSITIVE,
                analogCost,
                productType
        );
    }
}
