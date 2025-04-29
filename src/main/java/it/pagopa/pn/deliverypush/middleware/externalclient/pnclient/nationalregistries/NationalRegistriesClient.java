package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.CheckTaxIdOK;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressesRequestBody;

import java.time.Instant;
import java.util.List;

public interface NationalRegistriesClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES;
    String GET_DIGITAL_GENERAL_ADDRESS = "GET DIGITAL GENERAL ADDRESS";
    String CHECK_TAX_ID = "CHECK TAX ID";

    String GET_PHYSICAL_ADDRESSES = "GET PHYSICAL ADDRESSES";

    void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId, Instant notificationSentAt);

    CheckTaxIdOK checkTaxId(String taxId);

    List<NationalRegistriesResponse> sendRequestForGetPhysicalAddresses(PhysicalAddressesRequestBody physicalAddressesRequestBody);
}
 