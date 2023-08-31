package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class NotificationCancellationRequestDetailsInt implements TimelineElementDetailsInt {

    private String cancellationRequestId;

    public String toLog() {
        return String.format("cancellationRequestId=%s", cancellationRequestId);
    }
}
