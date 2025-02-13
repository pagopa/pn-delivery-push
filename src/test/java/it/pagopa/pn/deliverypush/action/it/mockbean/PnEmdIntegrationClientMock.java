package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;

public class PnEmdIntegrationClientMock implements PnEmdIntegrationClient {

    @Override
    public SendMessageResponse sendMessage(SendMessageRequestBody sendMessageRequest) {
        SendMessageResponse sendMessageResponse = new SendMessageResponse();
        sendMessageResponse.setOutcome(SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED);
        return sendMessageResponse;
    }
}
