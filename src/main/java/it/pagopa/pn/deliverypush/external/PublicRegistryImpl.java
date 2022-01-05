package it.pagopa.pn.deliverypush.external;

import org.springframework.stereotype.Component;

@Component
public class PublicRegistryImpl implements PublicRegistry {

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        throw new UnsupportedOperationException();
    }
}
