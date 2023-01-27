package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.ApiClient;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AddressApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBody;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBodyFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class PublicRegistryImpl extends CommonBaseClient implements PublicRegistry {

    private final PublicRegistryResponseHandler publicRegistryResponseHandler;

    private final PnDeliveryPushConfigs cfg;

    private AddressApi addressApi;

    public PublicRegistryImpl(@Lazy PublicRegistryResponseHandler publicRegistryResponseHandler,
                              PnDeliveryPushConfigs cfg) {
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
        this.cfg = cfg;
    }

    @PostConstruct
    public void init() {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( this.cfg.getSafeStorageBaseUrl() );
        addressApi = new AddressApi(newApiClient);
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId) {

        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId(taxId)
                .correlationId(correlationId)
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);


        addressApi.getAddresses(recipientType, new AddressRequestBody().filter(addressRequestBodyFilter))
                .doOnSuccess(addressOK -> log.info("Response of getAddresses with taxId: {}, correlationId: {}: {}", LogUtils.maskTaxId(taxId), correlationId, addressOK))
                .doOnError(throwable -> log.error(String.format("Error calling getAddresses with taxId: %s, correlationId: %s", LogUtils.maskTaxId(taxId), correlationId), throwable))
                .onErrorResume(throwable -> Mono.empty())
                .subscribe();

        log.info("sendRequestForGetDigitalAddress with correlationId: {} done", correlationId);

    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {         
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .physicalAddress(null)
                .build();

        publicRegistryResponseHandler.handleResponse(response);
    }

    public void setAddressApi(AddressApi addressApi) {
        this.addressApi = addressApi;
    }

}