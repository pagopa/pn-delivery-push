package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AddressApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressOK;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBody;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBodyFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;

class PublicRegistryImplTest {

    @Mock
    private PublicRegistryResponseHandler publicRegistryResponseHandler;

    private PublicRegistryImpl publicRegistry;

    @BeforeEach
    void setUp() {
        publicRegistryResponseHandler = Mockito.mock(PublicRegistryResponseHandler.class);
        PnDeliveryPushConfigs cfgMock = Mockito.mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfgMock.getNationalRegistriesBaseUrl()).thenReturn("localhost:8080");
        publicRegistry = new PublicRegistryImpl(publicRegistryResponseHandler, cfgMock);
    }

    @Test
    void sendRequestForGetDigitalAddressOK() {

        AddressApi addressApi = Mockito.mock(AddressApi.class);
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId("001")
                .correlationId("002")
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);
        Mockito.when(addressApi.getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter)))
                        .thenReturn(Mono.just(new AddressOK().correlationId("002")));
        publicRegistry.setAddressApi(addressApi);
        publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002");

        Mockito.verify(addressApi, Mockito.times(1)).getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter));
    }

    @Test
    void sendRequestForGetDigitalAddressKO() {

        AddressApi addressApi = Mockito.mock(AddressApi.class);
        AddressRequestBodyFilter addressRequestBodyFilter = new AddressRequestBodyFilter()
                .taxId("001")
                .correlationId("002")
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);
        Mockito.when(addressApi.getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter)))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "bad Gateway", null, null, Charset.defaultCharset())));
        publicRegistry.setAddressApi(addressApi);

        Assertions.assertDoesNotThrow(() -> publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002"));
        Mockito.verify(addressApi, Mockito.times(1)).getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter));
    }

    @Test
    void sendRequestForGetPhysicalAddress() {
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("002")
                .digitalAddress(null)
                .build();
        publicRegistry.sendRequestForGetPhysicalAddress("001", "002");

        Mockito.verify(publicRegistryResponseHandler, Mockito.times(1)).handleResponse(response);
    }
}