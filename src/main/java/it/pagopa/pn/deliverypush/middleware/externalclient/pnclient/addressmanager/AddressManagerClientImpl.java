package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.api.NormalizeAddressServiceApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsRequest;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class AddressManagerClientImpl extends CommonBaseClient implements AddressManagerClient {
    protected static final String PN_ADDRESS_MANAGER_CX_ID_VALUE = "pn-delivery-push";

    private final PnDeliveryPushConfigs cfg;
    private final NormalizeAddressServiceApi normalizeAddressServiceApi;

    @Override
    public Mono<AcceptedResponse> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, NORMALIZE_ADDRESS_PROCESS_NAME, normalizeItemsRequest.getCorrelationId());
        return normalizeAddressServiceApi.normalize(PN_ADDRESS_MANAGER_CX_ID_VALUE, cfg.getAddressManagerApiKey(), normalizeItemsRequest);
    }
    
}