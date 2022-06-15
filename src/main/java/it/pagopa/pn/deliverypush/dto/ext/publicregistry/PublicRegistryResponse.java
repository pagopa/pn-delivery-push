package it.pagopa.pn.deliverypush.dto.ext.publicregistry;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PublicRegistryResponse {
    private String correlationId;
    private LegalDigitalAddressInt digitalAddress;
    private PhysicalAddressInt physicalAddress;
}
