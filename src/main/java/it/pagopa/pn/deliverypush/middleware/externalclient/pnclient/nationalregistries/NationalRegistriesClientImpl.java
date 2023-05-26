package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@CustomLog
public class NationalRegistriesClientImpl extends CommonBaseClient implements NationalRegistriesClient {

    public static final String PN_NATIONAL_REGISTRIES_CX_ID_VALUE = "pn-delivery-push";
    
    private final AddressApi addressApi;
    private final AgenziaEntrateApi agenziaEntrateApi;

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, GET_DIGITAL_GENERAL_ADDRESS, correlationId);
        
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId(taxId)
                .correlationId(correlationId)
                .referenceRequestDate(LocalDate.now().toString()) //YYYY-MM-DD
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);

        MDCUtils.addMDCToContextAndExecute(
                addressApi.getAddresses(recipientType, new AddressRequestBody().filter(addressRequestBodyFilter), PN_NATIONAL_REGISTRIES_CX_ID_VALUE)
                        .doOnError(throwable -> log.error(String.format("Error calling getAddresses with taxId: %s, correlationId: %s", LogUtils.maskTaxId(taxId), correlationId), throwable))
        ).block();
        
    }

    @Override
    public CheckTaxIdOK checkTaxId(String taxId) {
        log.logInvokingExternalService(CLIENT_NAME, CHECK_TAX_ID);

        CheckTaxIdRequestBody checkTaxIdRequestBody = new CheckTaxIdRequestBody()
                .filter(
                    new CheckTaxIdRequestBodyFilter()
                            .taxId(taxId)
                );

        return MDCUtils.addMDCToContextAndExecute(
                agenziaEntrateApi.checkTaxId(checkTaxIdRequestBody)
        ).block();
    }
}