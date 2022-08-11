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
public class SendAnalogDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private ServiceLevelInt serviceLevel;
    private Integer sentAttemptMade;
    private Boolean investigation;
    private Integer numberOfPages;

    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d investigation=%s physicalAddress=%s",
                recIndex,
                sentAttemptMade,
                investigation,
                AuditLogUtils.SENSITIVE
        );
    }
}
