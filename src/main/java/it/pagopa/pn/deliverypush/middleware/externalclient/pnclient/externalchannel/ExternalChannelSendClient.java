package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;


import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;

public interface ExternalChannelSendClient {
    String CLIENT_NAME = "PN-EXTERNAL-CHANNEL";
    String LEGAL_NOTIFICATION_REQUEST = "LEGAL NOTIFICATION_REQUEST";
    String COURTESY_NOTIFICATION_REQUEST = "COURTESY NOTIFICATION_REQUEST";


    void sendLegalNotification(NotificationInt notificationInt,
                               NotificationRecipientInt recipientInt,
                               LegalDigitalAddressInt digitalAddress,
                               String timelineEventId,
                               String aarKey,
                               String quickAccessToken);

    void sendCourtesyNotification(NotificationInt notificationInt,
                                  NotificationRecipientInt recipientInt,
                                  CourtesyDigitalAddressInt digitalAddress,
                                  String timelineEventId,
                                  String aarKey,
                                  String quickAccessToken);

}
