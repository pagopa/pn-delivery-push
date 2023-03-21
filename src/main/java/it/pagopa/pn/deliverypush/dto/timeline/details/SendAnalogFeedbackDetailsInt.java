package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;

import java.time.Instant;
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
    private PhysicalAddressInt newAddress;
    private String deliveryFailureCause;
    private ResponseStatusInt responseStatus;
    private List<SendingReceipt> sendingReceipts;
    private String requestTimelineId;
    private String deliveryDetailCode;
    private Instant notificationDate;
    private List<AttachmentDetailsInt> attachments;

    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d responseStatus=%s deliveryFailureCause=%s " +
                        "physicalAddress=%s requestTimelineId=%s deliveryDetailCode=%s attachments=%s",
                recIndex,
                sentAttemptMade,
                responseStatus,
                deliveryFailureCause,
                AuditLogUtils.SENSITIVE,
                requestTimelineId,
                deliveryDetailCode,
                attachments
        );
    }
}
