package it.pagopa.pn.deliverypush.dto.address;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum DigitalAddressSourceInt {
    PLATFORM("PLATFORM"),

    SPECIAL("SPECIAL"),

    GENERAL("GENERAL");

    private final String value;

    DigitalAddressSourceInt(String value) {
        this.value = value;
    }

}
