package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsRequest;
import reactor.core.publisher.Mono;

public interface AddressManagerClient {
    String CLIENT_NAME = "PN-ADDRESS-MANAGER";
    String NORMALIZE_ADDRESS_PROCESS_NAME = "VALIDATE_AND_NORMALIZE_ADDRESS";

    Mono<AcceptedResponse> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest);
}
 