package it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status;

import lombok.Getter;

@Getter
public enum NotificationStatusInt {
    IN_VALIDATION("IN_VALIDATION", NotificationStatusInt.VERSION_10),

    ACCEPTED("ACCEPTED", NotificationStatusInt.VERSION_10),

    DELIVERING("DELIVERING", NotificationStatusInt.VERSION_10),

    DELIVERED("DELIVERED", NotificationStatusInt.VERSION_10),

    VIEWED("VIEWED", NotificationStatusInt.VERSION_10),

    EFFECTIVE_DATE("EFFECTIVE_DATE", NotificationStatusInt.VERSION_10),

    PAID("PAID", NotificationStatusInt.VERSION_10),

    UNREACHABLE("UNREACHABLE", NotificationStatusInt.VERSION_10),

    REFUSED("REFUSED", NotificationStatusInt.VERSION_10),

    CANCELLED("CANCELLED", NotificationStatusInt.VERSION_10),

    RETURNED_TO_SENDER("RETURNED_TO_SENDER", NotificationStatusInt.VERSION_26),

    DELIVERY_TIMEOUT("DELIVERY_TIMEOUT", NotificationStatusInt.VERSION_28);

    private final String value;
    private final int version;

    public static final int VERSION_10 = 10;
    public static final int VERSION_26 = 26;
    public static final int VERSION_28 = 28;

    NotificationStatusInt(String value, int version) {
        this.value = value;
        this.version = version;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
