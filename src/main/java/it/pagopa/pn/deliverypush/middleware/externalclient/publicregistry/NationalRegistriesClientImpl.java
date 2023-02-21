package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.ApiClient;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AddressApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Component
@Slf4j
public class NationalRegistriesClientImpl extends CommonBaseClient implements NationalRegistriesClient {

    protected static final String PN_NATIONAL_REGISTRIES_CX_ID_VALUE = "pn-delivery-push";

    private final PnDeliveryPushConfigs cfg;

    private AddressApi addressApi;
    private AgenziaEntrateApi agenziaEntrateApi;
    
    public NationalRegistriesClientImpl(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @PostConstruct
    public void init() {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( this.cfg.getNationalRegistriesBaseUrl() );
        addressApi = new AddressApi(newApiClient);
        agenziaEntrateApi = new AgenziaEntrateApi(newApiClient);
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId) {

        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId(taxId)
                .correlationId(correlationId)
                .referenceRequestDate(LocalDate.now().toString()) //YYYY-MM-DD
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);


        addressApi.getAddresses(recipientType, new AddressRequestBody().filter(addressRequestBodyFilter), PN_NATIONAL_REGISTRIES_CX_ID_VALUE)
                .doOnSuccess(addressOK -> log.info("Response of getAddresses with taxId: {}, correlationId: {}: {}", LogUtils.maskTaxId(taxId), correlationId, addressOK))
                .doOnError(throwable -> log.error(String.format("Error calling getAddresses with taxId: %s, correlationId: %s", LogUtils.maskTaxId(taxId), correlationId), throwable))
                .block();

        log.info("sendRequestForGetDigitalAddress with correlationId: {} done", correlationId);
    }

    @Override
    public CheckTaxIdOK checkTaxId(String taxId) {
        log.info("Start checkTaxId for taxId={}", LogUtils.maskTaxId(taxId));
        
        CheckTaxIdRequestBody checkTaxIdRequestBody = new CheckTaxIdRequestBody()
                .filter(
                    new CheckTaxIdRequestBodyFilter()
                            .taxId(taxId)
                );
        
        return agenziaEntrateApi.checkTaxId(checkTaxIdRequestBody)
                .doOnSuccess( res -> log.info("CheckTaxId completed for taxId={}", LogUtils.maskTaxId(taxId)))
                .block();
    }

    public void setAddressApi(AddressApi addressApi) {
        this.addressApi = addressApi;
    }

    public void setAgenziaEntrateApi(AgenziaEntrateApi agenziaEntrateApi) {
        this.agenziaEntrateApi = agenziaEntrateApi;
    }
}