package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.action2.it.TestUtils;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

public class PublicRegistryMock implements PublicRegistry {

    private final PublicRegistryResponseHandler publicRegistryResponseHandler;
    private final Map<String, DigitalAddress> digitalAddressResponse;
    private final Map<String, PhysicalAddress> physicalAddressResponse;




    public PublicRegistryMock(
            Map<String, DigitalAddress> digitalAddressResponse,
            Map<String, PhysicalAddress> physicalAddressResponse,
            PublicRegistryResponseHandler publicRegistryResponseHandler
                              ) {
        this.digitalAddressResponse = digitalAddressResponse;
        this.physicalAddressResponse = physicalAddressResponse;
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {

        DigitalAddress address = this.digitalAddressResponse.get( taxId );

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId( correlationId )
                .digitalAddress( address )
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        PhysicalAddress address = this.physicalAddressResponse.get( taxId );

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId( correlationId )
                .physicalAddress( address )
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

}
