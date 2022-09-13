package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaidDetails implements RecipientRelatedTimelineElementDetails {
    private int recIndex;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}
