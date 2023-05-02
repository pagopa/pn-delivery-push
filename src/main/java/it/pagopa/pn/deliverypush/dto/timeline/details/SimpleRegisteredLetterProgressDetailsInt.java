package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SimpleRegisteredLetterProgressDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant notificationDate;
    private String deliveryFailureCause;
    private String deliveryDetailCode;
    private List<AttachmentDetailsInt> attachments;
    private String sendRequestId;
    private String registeredLetterCode;
    
    public String toLog() {
        return String.format(
                "recIndex=%d notificationDate=%s deliveryFailureCause=%s deliveryDetailCode=%s attachments=%s",
                recIndex,
                notificationDate,
                deliveryFailureCause,
                deliveryDetailCode,
                attachments
        );
    }
}
