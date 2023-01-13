package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendAnalogFeedbackDetailsInt implements RecipientRelatedTimelineElementDetails, 
        NewAddressRelatedTimelineElement, PhysicalAddressRelatedTimelineElement {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private ServiceLevelInt serviceLevel;
    private Integer sentAttemptMade;
    private Boolean investigation;
    private PhysicalAddressInt newAddress;
    private List<String> errors = null;
    private ResponseStatusInt responseStatus;
    private List<SendingReceipt> sendingReceipts;
    private String requestTimelineId;

    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d responseStatus=%s errors=%s physicalAddress=%s requestTimelineId=%s",
                recIndex,
                sentAttemptMade,
                responseStatus,
                errors,
                AuditLogUtils.SENSITIVE,
                requestTimelineId
        );
    }
}
