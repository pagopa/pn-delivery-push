package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum ContactPhaseEntity {
    CHOOSE_DELIVERY("CHOOSE_DELIVERY"),

    SEND_ATTEMPT("SEND_ATTEMPT");

    private String value;

    ContactPhaseEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
