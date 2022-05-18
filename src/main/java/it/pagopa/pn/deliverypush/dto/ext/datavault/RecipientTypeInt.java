package it.pagopa.pn.deliverypush.dto.ext.datavault;

public enum RecipientTypeInt {
    PF("PF"),

    PG("PG");

    private final String value;

    RecipientTypeInt(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
