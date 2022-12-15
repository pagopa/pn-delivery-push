package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface ExternalChannelService {
    String sendDigitalNotification(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, Integer recIndex, int sentAttemptMade, boolean sendAlreadyInProgress);

    void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId);

}
