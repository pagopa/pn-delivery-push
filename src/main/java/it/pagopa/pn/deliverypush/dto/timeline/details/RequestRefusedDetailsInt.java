package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RequestRefusedDetailsInt implements TimelineElementDetailsInt {
    private List<NotificationRefusedErrorInt> refusalReasons;
    private Integer numberOfRecipients;
    private Integer notificationCost;
    private String notificationRequestId;
    private String paProtocolNumber;
    private String idempotenceToken;

    public String toLog() {
        return String.format(
                "errors=%s, notificationRequestId=%s, paProtocolNumber=%s, idempotenceToken=%s",
                refusalReasons,
                notificationRequestId,
                paProtocolNumber,
                idempotenceToken
        );
    }
}
