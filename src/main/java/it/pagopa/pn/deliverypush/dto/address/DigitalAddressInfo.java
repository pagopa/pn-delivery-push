package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalAddressInfo {
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private int sentAttemptMade;
    private Instant lastAttemptDate;
}
