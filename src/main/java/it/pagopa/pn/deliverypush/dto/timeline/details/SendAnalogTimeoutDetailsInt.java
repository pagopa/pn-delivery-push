package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendAnalogTimeoutDetailsInt extends BaseAnalogDetailsInt implements ElementTimestampTimelineElementDetails {

    private Instant timeoutDate;

    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d relatedRequestId=%s physicalAddress=%s timeoutDate=%s",
                recIndex,
                sentAttemptMade,
                relatedRequestId,
                AuditLogUtils.SENSITIVE,
                timeoutDate
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return timeoutDate;
    }

}

