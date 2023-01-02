package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder( toBuilder = true )
@EqualsAndHashCode
@ToString
public class NotificationViewedDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Integer notificationCost;
    private String raddType;
    private String raddTransactionId;
    private DelegateInfoInt delegateInfo;
    
    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}
