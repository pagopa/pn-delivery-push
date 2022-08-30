package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import lombok.Getter;

@Getter
public enum ExtChannelProgressEventCat {

    PROGRESS("PROGRESS"),

    OK("OK"),
    
    ERROR("ERROR");

    private final String value;

    ExtChannelProgressEventCat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
