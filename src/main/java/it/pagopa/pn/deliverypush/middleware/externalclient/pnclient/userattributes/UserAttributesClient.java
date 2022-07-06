package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserAttributesClient {
    ResponseEntity<List<LegalDigitalAddress>> getLegalAddressBySender(String internalId, String senderId);

    ResponseEntity<List<CourtesyDigitalAddress>> getCourtesyAddressBySender(String internalId, String senderId);
}
