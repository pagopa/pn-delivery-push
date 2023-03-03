package it.pagopa.pn.deliverypush.dto.io;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum IoSendMessageResultInt {
    NOT_SENT_OPTIN_ALREADY_SENT("NOT_SENT_OPTIN_ALREADY_SENT"),
    
    SENT_COURTESY("SENT_COURTESY"),

    SENT_OPTIN("SENT_OPTIN");
    
    private final String value;

    IoSendMessageResultInt(String value) {
        this.value = value;
    }
}
