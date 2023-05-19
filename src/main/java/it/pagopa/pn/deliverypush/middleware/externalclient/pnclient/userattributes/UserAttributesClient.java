package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import java.util.List;

public interface UserAttributesClient {
    String CLIENT_NAME = "PN-USER-ATTRIBUTES";

    List<LegalDigitalAddress> getLegalAddressBySender(String internalId, String senderId);

    List<CourtesyDigitalAddress> getCourtesyAddressBySender(String internalId, String senderId);
}
