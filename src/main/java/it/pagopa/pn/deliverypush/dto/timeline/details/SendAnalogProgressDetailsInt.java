package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendAnalogProgressDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant notificationDate;
    private String eventCode;
    private String eventDetail;

    public String toLog() {
        return String.format(
                "recIndex=%d eventCode=%s eventDetail=%s",
                recIndex,
                eventCode,
                eventDetail
        );
    }
}
