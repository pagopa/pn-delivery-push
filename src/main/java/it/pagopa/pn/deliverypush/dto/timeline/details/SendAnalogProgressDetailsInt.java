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
    private String deliveryFailureCause;
    private String deliveryDetailCode;

    public String toLog() {
        return String.format(
                "recIndex=%d notificationDate=%s deliveryFailureCause=%s deliveryDetailCode=%s",
                recIndex,
                notificationDate,
                deliveryFailureCause,
                deliveryDetailCode
        );
    }
}
