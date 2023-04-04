package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

public enum NotificationChannelType {

    SIMPLE_REGISTERED_LETTER("simple-registered-letter"),
    ANALOG_NOTIFICATION("analog-notification"),
    SIMPLE_REGISTERED_LETTER_AND_ANALOG_NOTIFICATION("simple-registered-letter-and-analog-notification");

    private final String value;

    NotificationChannelType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
