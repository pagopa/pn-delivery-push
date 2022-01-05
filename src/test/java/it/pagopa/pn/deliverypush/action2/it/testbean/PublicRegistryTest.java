package it.pagopa.pn.deliverypush.action2.it.testbean;

import it.pagopa.pn.deliverypush.external.PublicRegistry;

public class PublicRegistryTest implements PublicRegistry {

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String correlationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String taxId, String correlationId) {
        throw new UnsupportedOperationException();
    }
}
