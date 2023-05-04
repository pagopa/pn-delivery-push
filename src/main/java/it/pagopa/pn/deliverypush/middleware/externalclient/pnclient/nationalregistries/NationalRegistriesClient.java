package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.CheckTaxIdOK;

public interface NationalRegistriesClient {

    void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId);

    CheckTaxIdOK checkTaxId(String taxId);
}
 