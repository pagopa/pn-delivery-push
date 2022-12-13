package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;


import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;

public interface ExternalChannelSendClient {

    void sendLegalNotification(NotificationInt notificationInt,
                               NotificationRecipientInt recipientInt,
                               LegalDigitalAddressInt digitalAddress,
                               String timelineEventId,
                               String aarKey);

    void sendCourtesyNotification(NotificationInt notificationInt, 
                                  NotificationRecipientInt recipientInt,
                                  CourtesyDigitalAddressInt digitalAddress,
                                  String timelineEventId,
                                  String aarKey);

    void sendAnalogNotification(NotificationInt notificationInt,
                                NotificationRecipientInt recipientInt,
                                PhysicalAddressInt physicalAddress,
                                String timelineEventId,
                                PhysicalAddressInt.ANALOG_TYPE analogType,
                                String aarKey);
}
