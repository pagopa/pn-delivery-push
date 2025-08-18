package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.emdintegration;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.api.MessageApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@CustomLog
@Component
public class PnEmdIntegrationClientImpl extends CommonBaseClient implements PnEmdIntegrationClient {

    private final MessageApi messageApi;

    public SendMessageResponse sendMessage(SendMessageRequestBody sendMessageRequest) {
        log.logInvokingExternalService(CLIENT_NAME, SEND_MESSAGE);
        try {
            return messageApi.sendMessage(sendMessageRequest)
                    .block();
        } catch (Exception e) {
            log.error("Error sending message to EMD, fallback with NO_CHANNELS_ENABLED, message={}", e.getMessage());
            SendMessageResponse response = new SendMessageResponse();
            response.setOutcome(SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED);
            return response;
        }
    }
}
