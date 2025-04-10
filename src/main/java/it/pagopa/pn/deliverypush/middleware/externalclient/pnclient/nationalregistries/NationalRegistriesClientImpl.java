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

import java.time.Instant;

@Component
@RequiredArgsConstructor
@CustomLog
public class NationalRegistriesClientImpl extends CommonBaseClient implements NationalRegistriesClient {

    public static final String PN_NATIONAL_REGISTRIES_CX_ID_VALUE = "pn-delivery-push";
    public static final String PN_NATIONAL_REGISTRIES_CX_ID_VALUE_VALIDATION = "pn-delivery-push-validation";

    private final AddressApi addressApi;
    private final AgenziaEntrateApi agenziaEntrateApi;

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId, Instant notificationSentAt) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, GET_DIGITAL_GENERAL_ADDRESS, correlationId);

        //The instant.now has been replaced with notificationSentAt for the referenceRequestDate field, as the value of this field was not being used
        // for any logic either in delivery-push or in NationalRegistries within the digital flow.
        // For task PN-13423, it became necessary to populate it with notificationSentAt in order to apply the same logic used
        // in delivery-push for the feature flag enabling the new workflow for PF on national-registries.
        // RIPRISTINARE INSTANT.NOW ALLA RIMOZIONE DEL FEATURE FLAG PER IL NUOVO WORKFLOW DELLE PF
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId(taxId)
                .correlationId(correlationId)
                .referenceRequestDate(notificationSentAt)
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

    @Override
    public void sendRequestForGetPhysicalAddresses(PhysicalAddressesRequestBody physicalAddressesRequestBody) {
        String correlationId = physicalAddressesRequestBody.getCorrelationId();
        log.logInvokingExternalService(CLIENT_NAME, GET_PHYSICAL_ADDRESSES);

        MDCUtils.addMDCToContextAndExecute(
                addressApi.getPhysicalAddresses(physicalAddressesRequestBody, PN_NATIONAL_REGISTRIES_CX_ID_VALUE_VALIDATION)
                        .doOnError(throwable -> log.error(String.format("Error calling getPhysicalAddresses with correlationId: %s", correlationId), throwable)
        )).block();
    }
}