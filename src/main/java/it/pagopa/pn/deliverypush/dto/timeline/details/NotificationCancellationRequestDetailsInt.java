package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
public class NotificationCancellationRequestDetailsInt extends CategoryTypeTimelineElementDetailsInt implements TimelineElementDetailsInt {

    private String cancellationRequestId;

    public String toLog() {
        return String.format("cancellationRequestId=%s", cancellationRequestId);
    }
}
