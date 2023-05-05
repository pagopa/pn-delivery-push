package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
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
public class SendDigitalFeedbackDetailsInt implements DigitalAddressRelatedTimelineElement, DigitalAddressSourceRelatedTimelineElement {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private ResponseStatusInt responseStatus;
    private Instant notificationDate; //Cambiare il nome del campo in extChannelsFeedbackDate
    private List<SendingReceipt> sendingReceipts;
    private String requestTimelineId;
    private String deliveryFailureCause;
    private String deliveryDetailCode;

    public String toLog() {
        return String.format(
                "recIndex=%d responseStatus=%s deliveryFailureCause=%s digitalAddress=%s requestTimelineId=%s deliveryDetailCode=%s",
                recIndex,
                responseStatus,
                deliveryFailureCause,
                AuditLogUtils.SENSITIVE,
                requestTimelineId,
                deliveryDetailCode
        );
    }
}
