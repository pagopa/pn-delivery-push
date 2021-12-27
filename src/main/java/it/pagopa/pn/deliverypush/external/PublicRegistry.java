package it.pagopa.pn.deliverypush.external;

public interface PublicRegistry {
    //TODO Da implementare invio effettivo
    void sendRequest(String taxId, String correlationId);
}
