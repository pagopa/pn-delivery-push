package it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum NotificationStatusInt {
    IN_VALIDATION("IN_VALIDATION"),

    ACCEPTED("ACCEPTED"),

    DELIVERING("DELIVERING"),

    DELIVERED("DELIVERED"),

    VIEWED("VIEWED"),

    EFFECTIVE_DATE("EFFECTIVE_DATE"),

    PAID("PAID"),

    UNREACHABLE("UNREACHABLE"),

    REFUSED("REFUSED");

    private final String value;

    NotificationStatusInt(String value) {
        this.value = value;
    }
}
