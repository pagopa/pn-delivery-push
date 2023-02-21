package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class SendEventInt extends PaperEventInt {

    private String statusDescription;
    private List<AttachmentDetailsInt> attachments = null;
    private PhysicalAddressInt discoveredAddress;
    private String deliveryFailureCause;

}
