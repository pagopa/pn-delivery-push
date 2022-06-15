package it.pagopa.pn.deliverypush.dto.legalfacts;

import lombok.Getter;
import lombok.ToString;


@Getter
@ToString
public enum LegalFactCategoryInt {
    SENDER_ACK("SENDER_ACK"),

    DIGITAL_DELIVERY("DIGITAL_DELIVERY"),

    ANALOG_DELIVERY("ANALOG_DELIVERY"),

    RECIPIENT_ACCESS("RECIPIENT_ACCESS");

    private final String value;

    LegalFactCategoryInt(String value) {
        this.value = value;
    }

}
