package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PhysicalAddressInt {
    private String at;
    private String address;
    private String addressDetails;
    private String zip;
    private String municipality;
    private String municipalityDetails;
    private String province;
    private String foreignState;
}
