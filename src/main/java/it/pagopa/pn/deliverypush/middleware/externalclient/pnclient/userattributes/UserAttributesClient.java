package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalDigitalAddress;
import java.util.List;

public interface UserAttributesClient {
    String CLIENT_NAME = "PN-USER-ATTRIBUTES";
    String GET_DIGITAL_PLATFORM_ADDRESS = "GET DIGITAL PLATFORM ADDRESS";
    String GET_COURTESY_ADDRESS = "GET COURTESY ADDRESS";

    List<LegalDigitalAddress> getLegalAddressBySender(String internalId, String senderId);

    List<CourtesyDigitalAddress> getCourtesyAddressBySender(String internalId, String senderId);
}
