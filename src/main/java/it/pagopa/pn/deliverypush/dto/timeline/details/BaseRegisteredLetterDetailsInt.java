package it.pagopa.pn.deliverypush.dto.timeline.details;

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
public class BaseRegisteredLetterDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement {
    protected int recIndex;
    protected PhysicalAddressInt physicalAddress;
    protected String foreignState;

    public String toLog() {
        return String.format(
                "recIndex=%d physicalAddress=%s",
                recIndex,
                AuditLogUtils.SENSITIVE
        );
    }
}
