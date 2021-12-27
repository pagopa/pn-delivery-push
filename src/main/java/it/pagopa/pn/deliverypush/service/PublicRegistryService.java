package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;

public interface PublicRegistryService {
    void sendRequestForGetAddress(String iun, String taxId, String correlationId, DeliveryMode deliveryMode, ContactPhase contactPhase);
}
