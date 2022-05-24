package it.pagopa.pn.deliverypush.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.action2.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PublicRegistryImpl implements PublicRegistry {
    private final PublicRegistryResponseHandler publicRegistryResponseHandler;
    private final Map<String, DigitalAddress> digitalAddressResponse;
    private final Map<String, PhysicalAddress> physicalAddressResponse;

    //TODO La logica di public registry è MOCK quando verrà implementato il servizio bisognerà cambiarla

    public PublicRegistryImpl(
            Map<String, DigitalAddress> digitalAddressResponse,
            Map<String, PhysicalAddress> physicalAddressResponse,
            @Lazy PublicRegistryResponseHandler publicRegistryResponseHandler
    ) {
        this.digitalAddressResponse = digitalAddressResponse;
        this.physicalAddressResponse = physicalAddressResponse;
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DigitalAddress address = this.digitalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();

        publicRegistryResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PhysicalAddress address = this.physicalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .physicalAddress(address)
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }
}
