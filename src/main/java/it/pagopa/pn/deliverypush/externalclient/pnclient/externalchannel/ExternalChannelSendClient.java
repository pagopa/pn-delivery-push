package it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel;


import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

public interface ExternalChannelSendClient {

    enum ANALOG_TYPE{
        REGISTERED_LETTER_890,
        SIMPLE_REGISTERED_LETTER,
        AR_REGISTERED_LETTER
    }


    void sendDigitalNotification(NotificationInt notificationInt, DigitalAddress digitalAddress, String timelineEventId);

    void sendAnalogNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, String timelineEventId, ANALOG_TYPE analogType, String aarKey);
}
