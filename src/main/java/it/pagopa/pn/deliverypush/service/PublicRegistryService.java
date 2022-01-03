package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;

public interface PublicRegistryService {
    void sendRequestForGetDigitalAddress(String iun, String taxId, ContactPhase contactPhase, int sentAttemptMade);

    void sendRequestForGetPhysicalAddress(String iun, String taxId, int sentAttemptMade);
}
