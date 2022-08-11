package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
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
public class SendDigitalProgressDetailsInt implements RecipientRelatedTimelineElementDetails, DigitalAddressRelatedTimelineElement {
    private int recIndex;
    private ResponseStatusInt responseStatus;
    private LegalDigitalAddressInt digitalAddress;
    private Instant notificationDate;
    private List<SendingReceipt> sendingReceipts;

    public String toLog() {
        return String.format(
                "recIndex=%d responseStatus=%s digitalAddress=%s",
                recIndex,
                responseStatus,
                AuditLogUtils.SENSITIVE
        );
    }
}
