package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class PublicRegistryImplTest {

    @Mock
    private PublicRegistryResponseHandler publicRegistryResponseHandler;

    private PublicRegistryImpl publicRegistry;

    @BeforeEach
    void setUp() {
        publicRegistryResponseHandler = Mockito.mock(PublicRegistryResponseHandler.class);
        publicRegistry = new PublicRegistryImpl(publicRegistryResponseHandler);
    }

    @Test
    void sendRequestForGetDigitalAddress() {
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("002")
                .digitalAddress(null)
                .build();
        publicRegistry.sendRequestForGetDigitalAddress("001", "002");

        Mockito.verify(publicRegistryResponseHandler, Mockito.times(1)).handleResponse(response);
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