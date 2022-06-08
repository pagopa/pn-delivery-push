package it.pagopa.pn.deliverypush.dto.ext.publicregistry;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PublicRegistryResponse {
    private String correlationId;
    private LegalDigitalAddressInt digitalAddress;
    private PhysicalAddress physicalAddress;
}
