package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum NotificationRefusedErrorCodeEntity {
    FILE_NOTFOUND("FILE_NOTFOUND"),

    FILE_SHA_ERROR( "FILE_SHA_ERROR"),

    TAXID_NOT_VALID("TAXID_NOT_VALID");

    private final String value;

    NotificationRefusedErrorCodeEntity(String value) {
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
