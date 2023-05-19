package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface ExternalChannelService {
    String DIGITAL_LEGAL_PROCESS_NAME = "DIGITAL LEGAL NOTIFICATION";
    String DIGITAL_COURTESY_PROCESS_NAME = "DIGITAL COURTESY NOTIFICATION";

    String sendDigitalNotification(NotificationInt notification,
                                   Integer recIndex,
                                   boolean sendAlreadyInProgress,
                                   SendInformation sendInformation);

    void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId);

}
