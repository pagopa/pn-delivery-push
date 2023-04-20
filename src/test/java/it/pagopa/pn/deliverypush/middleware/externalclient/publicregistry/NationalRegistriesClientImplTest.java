package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClientImpl;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AddressApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.LocalDate;

class NationalRegistriesClientImplTest {

    private NationalRegistriesClientImpl publicRegistry;

    @BeforeEach
    void setUp() {
        PnDeliveryPushConfigs cfgMock = Mockito.mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfgMock.getNationalRegistriesBaseUrl()).thenReturn("localhost:8080");
        publicRegistry = new NationalRegistriesClientImpl(cfgMock);
    }

    @Test
    void sendRequestForGetDigitalAddressOK() {

        AddressApi addressApi = Mockito.mock(AddressApi.class);
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId("001")
                .correlationId("002")
                .referenceRequestDate(LocalDate.now().toString())
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);
        Mockito.when(addressApi.getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter), "pn-delivery-push"))
                        .thenReturn(Mono.just(new AddressOK().correlationId("002")));
        publicRegistry.setAddressApi(addressApi);
        publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002");

        Mockito.verify(addressApi, Mockito.times(1)).getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter), "pn-delivery-push");
    }

    @Test
    void sendRequestForGetDigitalAddressKO() {

        AddressApi addressApi = Mockito.mock(AddressApi.class);
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId("001")
                .correlationId("002")
                .referenceRequestDate(LocalDate.now().toString())
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);
        Mockito.when(addressApi.getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter), "pn-delivery-push"))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "bad Gateway", null, null, Charset.defaultCharset())));
        publicRegistry.setAddressApi(addressApi);

        Assertions.assertThrows(WebClientResponseException.BadGateway.class,
                () -> publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002"));
        Mockito.verify(addressApi, Mockito.times(1)).getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter), "pn-delivery-push");
    }

    @Test
    void checkTaxId() {
        //GIVEN
        final String taxIdTest = "TaxIdTest";

        AgenziaEntrateApi agenziaEntrateApi = Mockito.mock(AgenziaEntrateApi.class);
        CheckTaxIdOK checkTaxIdOK = new CheckTaxIdOK()
                .taxId(taxIdTest)
                .isValid(true);
        Mockito.when(agenziaEntrateApi.checkTaxId(Mockito.any(CheckTaxIdRequestBody.class)))
                .thenReturn(Mono.just(checkTaxIdOK));
        
        publicRegistry.setAgenziaEntrateApi(agenziaEntrateApi);

        //WHEN
        CheckTaxIdOK checkTaxIdOKResponse = publicRegistry.checkTaxId(taxIdTest);
        
        //THEN
        Assertions.assertNotNull(checkTaxIdOKResponse);
        Assertions.assertEquals(taxIdTest, checkTaxIdOKResponse.getTaxId());
        Assertions.assertEquals(Boolean.TRUE, checkTaxIdOKResponse.getIsValid());

    }

}