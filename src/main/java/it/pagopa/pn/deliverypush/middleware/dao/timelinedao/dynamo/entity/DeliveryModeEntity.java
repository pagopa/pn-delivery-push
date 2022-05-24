package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum DeliveryModeEntity {
    DIGITAL("DIGITAL"),

    ANALOG("ANALOG");

    private String value;

    DeliveryModeEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
