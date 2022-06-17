package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ExtChannelProgressEventCat {

    PROGRESS("PROGRESS"),

    OK("OK"),

    RETRIABLE_ERROR("RETRIABLE_ERROR"),

    ERROR("ERROR");

    private final String value;

    ExtChannelProgressEventCat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
}
