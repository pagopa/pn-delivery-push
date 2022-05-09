package it.pagopa.pn.deliverypush.dto.address;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalAddressInfoInternal {
    private DigitalAddress address;
    private DigitalAddressSource addressSource;
    private int sentAttemptMade;
    private Instant lastAttemptDate;
}
