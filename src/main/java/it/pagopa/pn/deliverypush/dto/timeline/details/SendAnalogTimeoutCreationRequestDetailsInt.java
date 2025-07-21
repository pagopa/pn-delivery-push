package it.pagopa.pn.deliverypush.dto.timeline.details;

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
public class SendAnalogTimeoutCreationRequestDetailsInt implements ElementTimestampTimelineElementDetails {
    private Instant timeoutDate;
    private Integer recIndex;
    private Integer sentAttemptMade;
    private String relatedRequestId; // si riferisce ad elementi di tipo SEND_ANALOG_DOMICILE
    private String legalFactId;

    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d relatedRequestId=%s timeoutDate=%s legalFactId=%s",
                recIndex,
                sentAttemptMade,
                relatedRequestId,
                timeoutDate,
                legalFactId
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return timeoutDate;
    }

}

