package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import reactor.core.publisher.Mono;

public interface AddressManagerService {
    Mono<AcceptedResponse> normalizeAddresses(NotificationInt listPhysicalAddress, String correlationId);
}
