package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PublicRegistryResponseDetailsInt implements RecipientRelatedTimelineElementDetails, DigitalAddressRelatedTimelineElement {
    protected int recIndex;
    protected LegalDigitalAddressInt digitalAddress;
    protected PhysicalAddressInt physicalAddress;
    protected String requestTimelineId;

    public String toLog() {
        return String.format(
                "recIndex=%d digitalAddress=%s physicalAddress=%s requestTimelineId=%s",
                recIndex,
                AuditLogUtils.SENSITIVE,
                AuditLogUtils.SENSITIVE,
                requestTimelineId
        );
    }
}
