package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.Getter;

@Getter
public enum ResultFilterEnumEntity {
    SUCCESS("SUCCESS"),

    DISCARD("DISCARD"),

    NEXT("NEXT");

    private final String value;

    ResultFilterEnumEntity(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

