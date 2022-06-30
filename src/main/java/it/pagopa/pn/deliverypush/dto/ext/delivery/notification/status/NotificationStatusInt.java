package it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status;

import lombok.Getter;

@Getter
public enum NotificationStatusInt {
    IN_VALIDATION("IN_VALIDATION"),

    ACCEPTED("ACCEPTED"),

    DELIVERING("DELIVERING"),

    DELIVERED("DELIVERED"),

    VIEWED("VIEWED"),

    EFFECTIVE_DATE("EFFECTIVE_DATE"),

    PAID("PAID"),

    UNREACHABLE("UNREACHABLE"),

    REFUSED("REFUSED"),

    CANCELLED("CANCELLED");

    private final String value;

    NotificationStatusInt(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
