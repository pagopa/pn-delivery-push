package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;

public interface PnExternalRegistryClient {
    String CLIENT_NAME = "PN-EXTERNAL-REGISTRIES";
    String SEND_IO_MESSAGE = "SEND MESSAGE TO IO";
    
    SendMessageResponse sendIOMessage(SendMessageRequest sendMessageRequest);
}
