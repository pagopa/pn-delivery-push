package it.pagopa.pn.deliverypush.actions2;


import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;

public interface PublicRegistryHandler {
    void sendNotification(String iun, String taxId, String correlationId, DeliveryMode deliveryMode, ContactPhase contactPhase);

    void handleResponse(PublicRegistryResponse response);

}
