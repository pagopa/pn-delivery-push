package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;

public interface PublicRegistryService {
    void sendRequestForGetDigitalGeneralAddress(NotificationInt notification, Integer recIndex, ContactPhaseInt contactPhase, int sentAttemptMade);

    void sendRequestForGetPhysicalAddress(NotificationInt notification, Integer recIndex, int sentAttemptMade);
}
