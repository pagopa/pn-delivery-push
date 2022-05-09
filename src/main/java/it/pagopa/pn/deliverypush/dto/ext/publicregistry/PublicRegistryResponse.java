package it.pagopa.pn.deliverypush.dto.ext.publicregistry;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PublicRegistryResponse {
    private String correlationId;
    private DigitalAddress digitalAddress;
    private PhysicalAddress physicalAddress;
}
