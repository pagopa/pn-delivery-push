package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.*;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Component
@CustomLog
public class NationalRegistriesClientImpl extends CommonBaseClient implements NationalRegistriesClient {

    public static final String PN_NATIONAL_REGISTRIES_CX_ID_VALUE = "pn-delivery-push";

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
        log.logInvokingAsyncExternalService(CLIENT_NAME, GET_DIGITAL_GENERAL_ADDRESS, correlationId);
        
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId(taxId)
                .correlationId(correlationId)
                .referenceRequestDate(LocalDate.now().toString()) //YYYY-MM-DD
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);


        addressApi.getAddresses(recipientType, new AddressRequestBody().filter(addressRequestBodyFilter), PN_NATIONAL_REGISTRIES_CX_ID_VALUE)
                .doOnError(throwable -> log.error(String.format("Error calling getAddresses with taxId: %s, correlationId: %s", LogUtils.maskTaxId(taxId), correlationId), throwable))
                .block();
    }

    @Override
    public CheckTaxIdOK checkTaxId(String taxId) {
        log.logInvokingExternalService(CLIENT_NAME, CHECK_TAX_ID);

        CheckTaxIdRequestBody checkTaxIdRequestBody = new CheckTaxIdRequestBody()
                .filter(
                    new CheckTaxIdRequestBodyFilter()
                            .taxId(taxId)
                );
        
        return agenziaEntrateApi.checkTaxId(checkTaxIdRequestBody)
                .block();
    }

    public void setAddressApi(AddressApi addressApi) {
        this.addressApi = addressApi;
    }

    public void setAgenziaEntrateApi(AgenziaEntrateApi agenziaEntrateApi) {
        this.agenziaEntrateApi = agenziaEntrateApi;
    }
}