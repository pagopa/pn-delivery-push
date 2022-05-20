package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum ServiceLevelEntity {
    SIMPLE_REGISTERED_LETTER("SIMPLE_REGISTERED_LETTER"),

    REGISTERED_LETTER_890("REGISTERED_LETTER_890");

    private String value;

    ServiceLevelEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
