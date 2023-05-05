package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SimpleRegisteredLetterDetailsInt extends BaseRegisteredLetterDetailsInt implements AnalogSendTimelineElement {

    private Integer analogCost;
    private String productType;
    private Integer numberOfPages;
    private Integer envelopeWeight;

    protected String prepareRequestId;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d physicalAddress=%s analogCost=%d productType=%s prepareRequestId=%s",
                recIndex,
                AuditLogUtils.SENSITIVE,
                analogCost,
                productType,
                prepareRequestId
        );
    }
}
