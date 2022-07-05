package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalRegistry;

import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;
import org.springframework.http.ResponseEntity;

public interface PnExternalRegistryClient {
    ResponseEntity<SendMessageResponse> sendIOMessage(SendMessageRequest sendMessageRequest);
}
