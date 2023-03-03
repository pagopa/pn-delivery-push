package it.pagopa.pn.deliverypush.dto.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IoSendMessageResultInt {
    NOT_SENT_OPTIN_ALREADY_SENT("NOT_SENT_OPTIN_ALREADY_SENT"),
    
    SENT_COURTESY("SENT_COURTESY"),

    SENT_OPTIN("SENT_OPTIN");
    
    private final String value;

    IoSendMessageResultInt(String value) {
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

    @JsonCreator
    public static IoSendMessageResultInt fromValue(String value) {
        for (IoSendMessageResultInt b : IoSendMessageResultInt.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
    
}
