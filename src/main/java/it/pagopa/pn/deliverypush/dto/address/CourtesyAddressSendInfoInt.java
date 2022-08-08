package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class CourtesyAddressSendInfoInt {
    private CourtesyDigitalAddressInt courtesyDigitalAddress;
    private Boolean sent;
}
