package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface ExternalChannelService {
    String sendDigitalNotification(NotificationInt notification,
                                   Integer recIndex,
                                   boolean sendAlreadyInProgress,
                                   SendInformation sendInformation);

    void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId);

}
