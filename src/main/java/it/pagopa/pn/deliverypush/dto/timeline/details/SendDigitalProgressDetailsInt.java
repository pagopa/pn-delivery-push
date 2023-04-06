package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
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
public class SendDigitalProgressDetailsInt implements DigitalSendTimelineElementDetails {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private Integer retryNumber;
    private Instant notificationDate;
    private List<SendingReceipt> sendingReceipts;
    private String eventCode;
    private boolean shouldRetry;
    private Boolean isFirstSendRetry;
    private String relatedFeedbackTimelineId;
    
    public String toLog() {
        return String.format(
                "recIndex=%d eventCode=%s digitalAddress=%s shouldRetry=%b digitalAddressSource=%s retryNumber=%d isFirstSendRetry=%s relatedFeedbackTimelineId=%s",
                recIndex,
                eventCode,
                AuditLogUtils.SENSITIVE,
                shouldRetry,
                digitalAddressSource.getValue(),
                retryNumber,
                isFirstSendRetry,
                relatedFeedbackTimelineId
        );
    }
}
