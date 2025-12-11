package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class NotificationRADDRetrievedDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails, ElementTimestampTimelineElementDetails{
    private int recIndex;
    private String raddType;
    private String raddTransactionId;
    private Instant eventTimestamp;
    
    public String toLog() {
        return String.format(
                "recIndex=%d eventTimestamp=%s",
                recIndex,
                eventTimestamp
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return eventTimestamp;
    }
}
