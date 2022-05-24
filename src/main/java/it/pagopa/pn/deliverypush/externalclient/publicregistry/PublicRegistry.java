package it.pagopa.pn.deliverypush.externalclient.publicregistry;

public interface PublicRegistry {
    //TODO Da implementare invio effettivo
    void sendRequestForGetDigitalAddress(String taxId, String correlationId);

    void sendRequestForGetPhysicalAddress(String taxId, String correlationId);
}
