package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum DigitalAddressSourceEntity {
    PLATFORM("PLATFORM"),

    SPECIAL("SPECIAL"),

    GENERAL("GENERAL");

    private final String value;

    DigitalAddressSourceEntity(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
