package it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel;


import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddressInt;

public interface ExternalChannelSendClient {

    // TODO da spostare in PhysicalAddressInt quando e se ci sarà
    enum ANALOG_TYPE{
        REGISTERED_LETTER_890,
        SIMPLE_REGISTERED_LETTER,
        AR_REGISTERED_LETTER
    }


    void sendLegalNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, LegalDigitalAddressInt digitalAddress, String timelineEventId);

    void sendCourtesyNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, CourtesyDigitalAddressInt digitalAddress, String timelineEventId);

    void sendAnalogNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, PhysicalAddressInt physicalAddress, String timelineEventId, ANALOG_TYPE analogType, String aarKey);
}
