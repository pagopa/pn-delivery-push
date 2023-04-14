package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
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
public class PrepareDigitalDetailsInt implements DigitalSendTimelineElementDetails {
    private int recIndex;

    // info relative a lastAddress
    private Integer retryNumber;
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private Instant attemptDate;

    // info relative a nextAddress
    private DigitalAddressSourceInt nextDigitalAddressSource;
    private int nextSourceAttemptsMade;
    private Instant nextLastAttemptMadeForSource;
    private Boolean isFirstSendRetry;
    private String relatedFeedbackTimelineId;
    
    public String toLog() {
        return String.format(
                "recIndex=%d source=%s retryNumber=%s digitalAddress=%s nextDigitalAddressSource=%s nextSourceAttemptsMade=%d lastAttemptMadeForSource=%s",
                recIndex,
                digitalAddressSource,
                retryNumber,
                AuditLogUtils.SENSITIVE,
                nextDigitalAddressSource,
                nextSourceAttemptsMade,
                nextLastAttemptMadeForSource
        );
    }
}
