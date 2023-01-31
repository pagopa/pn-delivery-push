package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

public interface PublicRegistry {

    void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId);

}
 