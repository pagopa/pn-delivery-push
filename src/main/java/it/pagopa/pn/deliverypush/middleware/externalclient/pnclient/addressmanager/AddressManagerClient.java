package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsRequest;
import reactor.core.publisher.Mono;

public interface AddressManagerClient {
    Mono<AcceptedResponse> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest);
}
 