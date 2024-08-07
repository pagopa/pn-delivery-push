package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder(toBuilder = true)
public class PrepareEventInt extends PaperEventInt {
    private PhysicalAddressInt receiverAddress;
    private List<String> replacedF24AttachmentUrls;
    private CategorizedAttachmentsResultInt categorizedAttachmentsResult;
    private String productType;
    private String failureDetailCode;

    public enum STATUS_CODE{
        OK,
        PROGRESS,
        KO
    }
}
