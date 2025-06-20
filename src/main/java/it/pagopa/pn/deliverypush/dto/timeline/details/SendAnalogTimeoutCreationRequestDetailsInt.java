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
public class SendAnalogTimeoutCreationRequestDetailsInt extends BaseAnalogDetailsInt implements ElementTimestampTimelineElementDetails {

    private Instant notificationDate;

    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d relatedRequestId=%s physicalAddress=%s notificationDate=%s",
                recIndex,
                sentAttemptMade,
                relatedRequestId,
                AuditLogUtils.SENSITIVE,
                notificationDate
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return notificationDate;
    }

}
