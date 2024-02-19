package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;


import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;

import java.util.List;

public interface ExternalChannelSendClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS;
    String LEGAL_NOTIFICATION_REQUEST = "LEGAL NOTIFICATION_REQUEST";
    String COURTESY_NOTIFICATION_REQUEST = "COURTESY NOTIFICATION_REQUEST";


    void sendLegalNotification(NotificationInt notificationInt,
                               NotificationRecipientInt recipientInt,
                               LegalDigitalAddressInt digitalAddress,
                               String timelineEventId,
                               List<String> fileKeys,
                               String quickAccessToken);

    void sendCourtesyNotification(NotificationInt notificationInt,
                                  NotificationRecipientInt recipientInt,
                                  CourtesyDigitalAddressInt digitalAddress,
                                  String timelineEventId,
                                  String aarKey,
                                  String quickAccessToken);

}
