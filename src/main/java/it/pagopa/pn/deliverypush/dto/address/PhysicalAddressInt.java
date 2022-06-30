package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Setter
@Getter
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
    
    public enum ANALOG_TYPE{
        REGISTERED_LETTER_890,
        SIMPLE_REGISTERED_LETTER,
        AR_REGISTERED_LETTER
    }
}
