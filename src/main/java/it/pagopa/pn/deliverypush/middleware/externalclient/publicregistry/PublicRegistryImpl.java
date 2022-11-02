package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PublicRegistryImpl implements PublicRegistry {
    //In attesa della risoluzione della PN-1145, la richiesta a PublicRegistry restituisce sempre una risposta vuota
    
    private final PublicRegistryResponseHandler publicRegistryResponseHandler;

    public PublicRegistryImpl(@Lazy PublicRegistryResponseHandler publicRegistryResponseHandler) {
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {         
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(null)
                .build();

        publicRegistryResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {         
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .physicalAddress(null)
                .build();

        publicRegistryResponseHandler.handleResponse(response);
    }
}