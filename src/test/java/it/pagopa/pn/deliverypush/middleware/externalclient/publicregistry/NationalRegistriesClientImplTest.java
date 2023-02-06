package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.api.AddressApi;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressOK;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBody;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressRequestBodyFilter;
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
                .referenceRequestDate(LocalDate.now().toString())
                .domicileType(AddressRequestBodyFilter.DomicileTypeEnum.DIGITAL);
        Mockito.when(addressApi.getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter)))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "bad Gateway", null, null, Charset.defaultCharset())));
        publicRegistry.setAddressApi(addressApi);

        Assertions.assertThrows(WebClientResponseException.BadGateway.class,
                () -> publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002"));
        Mockito.verify(addressApi, Mockito.times(1)).getAddresses("PF", new AddressRequestBody().filter(addressRequestBodyFilter));
    }

}