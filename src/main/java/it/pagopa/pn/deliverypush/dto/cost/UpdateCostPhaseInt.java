package it.pagopa.pn.deliverypush.dto.cost;

public enum UpdateCostPhaseInt {
    VALIDATION("VALIDATION"),

    REQUEST_REFUSED("REQUEST_REFUSED"),

    NOTIFICATION_CANCELLED("NOTIFICATION_CANCELLED");

    private final String value;

    UpdateCostPhaseInt(String value) {
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
