package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.ApiClient;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.api.NormalizeAddressServiceApi;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsRequest;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@Setter
@Slf4j
public class AddressManagerClientImpl extends CommonBaseClient implements AddressManagerClient {
    protected static final String PN_ADDRESS_MANAGER_CX_ID_VALUE = "pn-delivery-push";

    private final PnDeliveryPushConfigs cfg;

    private NormalizeAddressServiceApi normalizeAddressServiceApi;
    
    public AddressManagerClientImpl(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @PostConstruct
    public void init() {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( this.cfg.getNationalRegistriesBaseUrl() );
        normalizeAddressServiceApi = new NormalizeAddressServiceApi(newApiClient);
    }

    @Override
    public Mono<AcceptedResponse> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest) {
        return normalizeAddressServiceApi.normalize(PN_ADDRESS_MANAGER_CX_ID_VALUE, cfg.getAddressManagerApiKey(), normalizeItemsRequest)
                .doOnSuccess( res -> log.info("NormalizeAddresses completed correlationId={}", normalizeItemsRequest.getCorrelationId()));
    }
    
}