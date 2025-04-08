package it.pagopa.pn.deliverypush.dto.ext.datavault;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class NotificationRecipientAddressesDtoInt {
    private String denomination;
    private DigitalAddressInt digitalAddress;
    private PhysicalAddressInt physicalAddress;
    private Integer recIndex;
}
