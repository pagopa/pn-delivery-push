package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AnalogWorfklowRecipientDeceasedDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement, ElementTimestampTimelineElementDetails {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private Integer notificationCost;
    private Instant notificationDate;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d notificationCost=%d notificationDate=%s physicalAddress=%s",
                recIndex,
                notificationCost,
                notificationDate,
                AuditLogUtils.SENSITIVE
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return notificationDate;
    }

    @Override
    public void setPhysicalAddress(PhysicalAddressInt physicalAddressInt) {
        this.physicalAddress = physicalAddressInt;
    }
}