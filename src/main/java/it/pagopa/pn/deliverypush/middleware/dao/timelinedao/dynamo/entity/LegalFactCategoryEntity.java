package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum LegalFactCategoryEntity {
    SENDER_ACK("SENDER_ACK"),

    DIGITAL_DELIVERY("DIGITAL_DELIVERY"),

    ANALOG_DELIVERY("ANALOG_DELIVERY"),

    RECIPIENT_ACCESS("RECIPIENT_ACCESS");

    private String value;

    LegalFactCategoryEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
