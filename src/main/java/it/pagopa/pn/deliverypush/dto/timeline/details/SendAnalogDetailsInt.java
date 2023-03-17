package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder( toBuilder = true )
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SendAnalogDetailsInt extends BaseAnalogDetailsInt implements AnalogSendTimelineElement {

    private Integer analogCost;
    private String productType;
    private Integer numberOfPages;
    private Integer envelopeWeight;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d relatedRequestId=%s physicalAddress=%s analogCost=%d productType=%s",
                recIndex,
                sentAttemptMade,
                relatedRequestId,
                AuditLogUtils.SENSITIVE,
                analogCost,
                productType
        );
    }
}
