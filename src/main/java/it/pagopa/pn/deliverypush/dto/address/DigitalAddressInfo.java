package it.pagopa.pn.deliverypush.dto.address;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalAddressInfo {
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSource digitalAddressSource;
    private int sentAttemptMade;
    private Instant lastAttemptDate;
}
