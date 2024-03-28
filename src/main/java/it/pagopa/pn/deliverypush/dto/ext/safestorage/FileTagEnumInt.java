package it.pagopa.pn.deliverypush.dto.ext.safestorage;

import lombok.Getter;

@Getter
public enum FileTagEnumInt {
    DOCUMENT("DOCUMENT"),

    ATTACHMENT_PAGOPA("ATTACHMENT_PAGOPA"),

    ATTACHMENT_F24("ATTACHMENT_F24"),

    LEGAL_FACT("LEGAL_FACT"),

    LEGAL_FACT_EXTERNAL("LEGAL_FACT_EXTERNAL"),

    AAR("AAR");

    private final String value;

    FileTagEnumInt(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
