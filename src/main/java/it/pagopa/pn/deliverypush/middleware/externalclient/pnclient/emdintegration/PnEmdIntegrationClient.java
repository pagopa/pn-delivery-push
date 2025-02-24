package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.emdintegration;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageResponse;

public interface PnEmdIntegrationClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EMD_INTEGRATION;
    String SEND_MESSAGE = "sendMessage";
    SendMessageResponse sendMessage(SendMessageRequestBody sendMessageRequest);
}
