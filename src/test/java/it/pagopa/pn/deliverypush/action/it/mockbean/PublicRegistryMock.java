package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.PublicRegistry;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PublicRegistryMock implements PublicRegistry {

    private final PublicRegistryResponseHandler publicRegistryResponseHandler;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponse;
    private ConcurrentMap<String, PhysicalAddressInt> physicalAddressResponse;


    public PublicRegistryMock(
            PublicRegistryResponseHandler publicRegistryResponseHandler
    ) {
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
    }

    public void clear() {
        this.digitalAddressResponse = new ConcurrentHashMap<>();
        this.physicalAddressResponse = new ConcurrentHashMap<>();
    }

    public void addDigital(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponse.put(key,value);
    }

    public void addPhysical(String key, PhysicalAddressInt value) {
        this.physicalAddressResponse.put(key,value);
    }
    
    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {
        new Thread(() -> {
            Assertions.assertDoesNotThrow(() -> {
                simulateDigitalAddressResponse(taxId, correlationId);
            });
        }).start();
    }

    private void simulateDigitalAddressResponse(String taxId, String correlationId) {
        LegalDigitalAddressInt address = this.digitalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        new Thread(() -> {
            Assertions.assertDoesNotThrow(() -> {
                simulatePhysicalAddressResponse(taxId, correlationId);
            });
        }).start();
    }

    private void simulatePhysicalAddressResponse(String taxId, String correlationId) {
        PhysicalAddressInt address = this.physicalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .physicalAddress(address)
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

}
