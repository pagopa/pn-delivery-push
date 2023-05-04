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
public class NormalizedAddressDetailsInt implements RecipientRelatedTimelineElementDetails, NewAddressRelatedTimelineElement, PhysicalAddressRelatedTimelineElement{
    private int recIndex;
    private PhysicalAddressInt oldAddress;
    private PhysicalAddressInt normalizedAddress;

    public String toLog() {
        return String.format(
                "recIndex=%d oldPhysicalAddress%s normalizedAddress=%s",
                recIndex,
                AuditLogUtils.SENSITIVE,
                AuditLogUtils.SENSITIVE
        );
    }

    @Override
    public PhysicalAddressInt getNewAddress() {
        return normalizedAddress;
    }

    @Override
    public void setNewAddress(PhysicalAddressInt physicalAddress) {
        this.normalizedAddress = physicalAddress;
    }

    @Override
    public PhysicalAddressInt getPhysicalAddress() {
        return oldAddress;
    }

    @Override
    public void setPhysicalAddress(PhysicalAddressInt physicalAddressInt) {
        this.oldAddress = physicalAddressInt;
    }
}