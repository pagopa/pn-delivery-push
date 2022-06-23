package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum ResponseStatusEntity {
    OK("OK"),

    KO("KO");

    private final String value;

    ResponseStatusEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
