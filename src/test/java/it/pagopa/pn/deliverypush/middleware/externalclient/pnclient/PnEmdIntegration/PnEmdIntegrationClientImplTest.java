package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.PnEmdIntegration;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.api.MessageApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.model.SendMessageResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class PnEmdIntegrationClientImplTest {

    private MessageApi messageApi;
    private PnEmdIntegrationClientImpl pnEmdIntegrationClient;

    @BeforeEach
    void setup() {
        messageApi = Mockito.mock(MessageApi.class);
        pnEmdIntegrationClient = new PnEmdIntegrationClientImpl(messageApi);
    }

    @Test
    void sendMessage_successfulResponse() {
        SendMessageRequestBody request = new SendMessageRequestBody();
        SendMessageResponse expectedResponse = new SendMessageResponse();
        expectedResponse.setOutcome(SendMessageResponse.OutcomeEnum.OK);
        Mockito.when(messageApi.sendMessage(Mockito.any(SendMessageRequestBody.class)))
                .thenReturn(Mono.just(expectedResponse));

        SendMessageResponse actualResponse = pnEmdIntegrationClient.sendMessage(request);
        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void sendMessage_exceptionThrown() {
        SendMessageRequestBody request = new SendMessageRequestBody();
        Mockito.when(messageApi.sendMessage(Mockito.any(SendMessageRequestBody.class)))
                .thenReturn(Mono.error(new RuntimeException("Test exception")));

        SendMessageResponse actualResponse = pnEmdIntegrationClient.sendMessage(request);
        Assertions.assertEquals(SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED, actualResponse.getOutcome());
    }
}