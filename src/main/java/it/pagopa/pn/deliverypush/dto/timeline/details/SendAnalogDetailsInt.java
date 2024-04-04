package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

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
    private String prepareRequestId;
    private List<String> f24Attachments;
    private CategorizedAttachmentsResultInt categorizedAttachmentsResult;
    private Integer vat;
    
    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d sentAttemptMade=%d relatedRequestId=%s physicalAddress=%s analogCost=%d productType=%s prepareRequestId=%s f24Attachments=%s, vat=%s",
                recIndex,
                sentAttemptMade,
                relatedRequestId,
                AuditLogUtils.SENSITIVE,
                analogCost,
                productType,
                prepareRequestId,
                !CollectionUtils.isEmpty(f24Attachments) ? String.join(",", f24Attachments) : Collections.emptyList(),
                vat
        );
    }
}
