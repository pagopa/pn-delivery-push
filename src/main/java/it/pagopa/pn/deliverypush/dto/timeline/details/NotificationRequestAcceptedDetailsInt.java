package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class NotificationRequestAcceptedDetailsInt extends GenericTimelineElementDetailsInt implements TimelineElementDetailsInt{
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

