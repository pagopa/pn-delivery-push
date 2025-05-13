package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationRequestAcceptedDetailsInt implements TimelineElementDetailsInt{
    private String notificationRequestId;
    private String paProtocolNumber;
    private String idempotenceToken;

    public String toLog() {
        return String.format("notificationRequestId=%s, paProtocolNumber=%s, idempotenceToken=%s",
                notificationRequestId,
                paProtocolNumber,
                idempotenceToken
        );
    }
}

