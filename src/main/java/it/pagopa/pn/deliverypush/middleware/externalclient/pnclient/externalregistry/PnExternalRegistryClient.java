package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;

public interface PnExternalRegistryClient {
    String CLIENT_NAME = "PN-EXTERNAL-REGISTRIES";
    String SEND_IO_MESSAGE = "SEND MESSAGE TO IO";
    
    SendMessageResponse sendIOMessage(SendMessageRequest sendMessageRequest);
}
