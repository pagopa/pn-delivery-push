package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder(toBuilder = true)
public class PrepareEventInt extends PaperEventInt {
    private PhysicalAddressInt receiverAddress;
    private String productType;

    public enum STATUS_CODE{
        OK,
        PROGRESS,
        KOUNREACHABLE
    }
}
