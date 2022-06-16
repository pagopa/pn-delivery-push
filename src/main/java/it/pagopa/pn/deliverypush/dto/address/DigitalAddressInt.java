package it.pagopa.pn.deliverypush.dto.address;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@ToString
@SuperBuilder( toBuilder = true )
public abstract class DigitalAddressInt {
    private String address;
}
