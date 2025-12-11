package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class SimpleRegisteredLetterProgressDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails, ElementTimestampTimelineElementDetails {
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

    @Override
    public Instant getElementTimestamp() {
        return notificationDate;
    }
}
