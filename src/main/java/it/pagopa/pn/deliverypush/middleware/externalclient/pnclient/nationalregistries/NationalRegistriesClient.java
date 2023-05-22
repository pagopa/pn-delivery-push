package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.CheckTaxIdOK;

public interface NationalRegistriesClient {
    String CLIENT_NAME = "PN-NATIONAL-REGISTRIES";
    String GET_DIGITAL_GENERAL_ADDRESS = "GET DIGITAL GENERAL ADDRESS";
    String CHECK_TAX_ID = "CHECK TAX ID";

    void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId);

    CheckTaxIdOK checkTaxId(String taxId);
}
 