package it.pagopa.pn.deliverypush.dto.timeline.details;

public enum NotificationRefusedErrorCodeInt {
    FILE_NOTFOUND("FILE_NOTFOUND"),

    FILE_SHA_ERROR( "FILE_SHA_ERROR"),

    TAXID_NOT_VALID("TAXID_NOT_VALID"),

    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE");
    
    private final String value;

    NotificationRefusedErrorCodeInt(String value) {
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
