package it.pagopa.pn.deliverypush.dto.ext.publicregistry;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class NationalRegistriesResponse {
    private String correlationId;
    private LegalDigitalAddressInt digitalAddress;
    private PhysicalAddressInt physicalAddress;
    private Integer recIndex;
    private String registry;
    private Instant addressResolutionStart;
    private Instant addressResolutionEnd;
}
