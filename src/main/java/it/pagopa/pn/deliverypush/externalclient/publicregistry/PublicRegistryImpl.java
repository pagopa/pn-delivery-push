package it.pagopa.pn.deliverypush.externalclient.publicregistry;

import org.springframework.stereotype.Component;

@Component
public class PublicRegistryImpl implements PublicRegistry {
    //FIXME In attesa della risoluzione della PN-1145, la richiesta a PublicRegistry comporta una UnsupportedOperationException
    
    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        throw new UnsupportedOperationException();
    }
}