package it.pagopa.pn.deliverypush.dto.legalfacts;

import lombok.Getter;


@Getter
public enum LegalFactCategoryInt {
    SENDER_ACK("SENDER_ACK"),

    DIGITAL_DELIVERY("DIGITAL_DELIVERY"),

    ANALOG_DELIVERY("ANALOG_DELIVERY"),

    RECIPIENT_ACCESS("RECIPIENT_ACCESS"),

    PEC_RECEIPT("PEC_RECEIPT");

    private final String value;

    LegalFactCategoryInt(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
