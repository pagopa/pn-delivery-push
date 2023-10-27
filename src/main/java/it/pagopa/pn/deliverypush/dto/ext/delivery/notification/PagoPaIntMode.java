package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PagoPaIntMode {
    NONE("NONE"),

    SYNC("SYNC"),

    ASYNC("ASYNC");

    private final String value;

    PagoPaIntMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
