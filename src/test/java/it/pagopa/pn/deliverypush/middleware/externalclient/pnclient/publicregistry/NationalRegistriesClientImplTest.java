package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.publicregistry;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressOK;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.CheckTaxIdOK;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.CheckTaxIdRequestBody;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

class NationalRegistriesClientImplTest {

    private NationalRegistriesClientImpl publicRegistry;
    private AddressApi addressApi = Mockito.mock(AddressApi.class);
    private AgenziaEntrateApi agenziaEntrateApi = Mockito.mock(AgenziaEntrateApi.class);

    @BeforeEach
    void setUp() {
        publicRegistry = new NationalRegistriesClientImpl(addressApi, agenziaEntrateApi);
    }

    @Test
    void sendRequestForGetDigitalAddressOK() {
        Mockito.when(addressApi.getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push")))
                        .thenReturn(Mono.just(new AddressOK().correlationId("002")));
        
        publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002");

        Mockito.verify(addressApi, Mockito.times(1)).getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push"));
    }

    @Test
    void sendRequestForGetDigitalAddressKO() {
        Mockito.when(addressApi.getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push")))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "bad Gateway", null, null, Charset.defaultCharset())));

        Assertions.assertThrows(WebClientResponseException.BadGateway.class,
                () -> publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002"));
        Mockito.verify(addressApi, Mockito.times(1)).getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push"));
    }

    @Test
    void checkTaxId() {
        //GIVEN
        final String taxIdTest = "TaxIdTest";

        CheckTaxIdOK checkTaxIdOK = new CheckTaxIdOK()
                .taxId(taxIdTest)
                .isValid(true);
        Mockito.when(agenziaEntrateApi.checkTaxId(Mockito.any(CheckTaxIdRequestBody.class)))
                .thenReturn(Mono.just(checkTaxIdOK));

        //WHEN
        CheckTaxIdOK checkTaxIdOKResponse = publicRegistry.checkTaxId(taxIdTest);
        
        //THEN
        Assertions.assertNotNull(checkTaxIdOKResponse);
        Assertions.assertEquals(taxIdTest, checkTaxIdOKResponse.getTaxId());
        Assertions.assertEquals(Boolean.TRUE, checkTaxIdOKResponse.getIsValid());

    }

}