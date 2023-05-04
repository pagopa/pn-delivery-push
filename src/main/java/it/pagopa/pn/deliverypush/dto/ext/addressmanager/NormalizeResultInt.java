package it.pagopa.pn.deliverypush.dto.ext.addressmanager;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class NormalizeResultInt {
    private String id;
    private PhysicalAddressInt normalizedAddress;
    private String error;

}
