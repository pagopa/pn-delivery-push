package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsRequest;
import reactor.core.publisher.Mono;

public interface AddressManagerClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_ADDRESS_MANAGER;
    
    String NORMALIZE_ADDRESS_PROCESS_NAME = "VALIDATE AND NORMALIZE ADDRESS";

    Mono<AcceptedResponse> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest);
}
 