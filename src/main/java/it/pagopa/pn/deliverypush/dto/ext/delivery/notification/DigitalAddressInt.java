package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode
@ToString
@SuperBuilder
public abstract class DigitalAddressInt {

    private String address;
}
