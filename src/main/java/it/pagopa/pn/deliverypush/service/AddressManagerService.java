package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import reactor.core.publisher.Mono;

public interface AddressManagerService {
    String VALIDATE_AND_NORMALIZE_ADDRESS_PROCESS_NAME = "VALIDATE AND NORMALIZE ADDRESS";

    Mono<AcceptedResponse> normalizeAddresses(NotificationInt notification, String correlationId);
}
