package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.externalclient.publicregistry.PublicRegistry;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;

import java.util.HashMap;
import java.util.Map;

public class PublicRegistryMock implements PublicRegistry {

    private final PublicRegistryResponseHandler publicRegistryResponseHandler;
    private Map<String, LegalDigitalAddressInt> digitalAddressResponse;
    private Map<String, PhysicalAddress> physicalAddressResponse;


    public PublicRegistryMock(
            PublicRegistryResponseHandler publicRegistryResponseHandler
    ) {
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
    }

    public void clear() {
        this.digitalAddressResponse = new HashMap<>();
        this.physicalAddressResponse = new HashMap<>();
    }

    public void addDigital(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponse.put(key,value);
    }

    public void addPhysical(String key, PhysicalAddress value) {
        this.physicalAddressResponse.put(key,value);
    }
    
    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {

        LegalDigitalAddressInt address = this.digitalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        PhysicalAddress address = this.physicalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .physicalAddress(address)
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

}
