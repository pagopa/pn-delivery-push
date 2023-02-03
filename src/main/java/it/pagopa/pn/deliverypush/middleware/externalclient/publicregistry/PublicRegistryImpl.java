package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.ApiClient;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AddressApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBody;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBodyFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Component
@Slf4j
public class PublicRegistryImpl extends NationalRegistriesBaseClient implements PublicRegistry {

    private final PnDeliveryPushConfigs cfg;

    private AddressApi addressApi;

    public PublicRegistryImpl(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @PostConstruct
    public void init() {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( this.cfg.getNationalRegistriesBaseUrl() );
        addressApi = new AddressApi(newApiClient);
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId) {

        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId(taxId)
                .correlationId(correlationId)
                .referenceRequestDate(LocalDate.now().toString()) //YYYY-MM-DD
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);


        addressApi.getAddresses(recipientType, new AddressRequestBody().filter(addressRequestBodyFilter))
                .doOnSuccess(addressOK -> log.info("Response of getAddresses with taxId: {}, correlationId: {}: {}", LogUtils.maskTaxId(taxId), correlationId, addressOK))
                .doOnError(throwable -> log.error(String.format("Error calling getAddresses with taxId: %s, correlationId: %s", LogUtils.maskTaxId(taxId), correlationId), throwable))
                .block();

        log.info("sendRequestForGetDigitalAddress with correlationId: {} done", correlationId);

    }

    public void setAddressApi(AddressApi addressApi) {
        this.addressApi = addressApi;
    }

}