package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import reactor.core.publisher.Mono;

public interface AddressManagerService {
    Mono<AcceptedResponse> normalizeAddresses(NotificationInt notification, String correlationId);
}
