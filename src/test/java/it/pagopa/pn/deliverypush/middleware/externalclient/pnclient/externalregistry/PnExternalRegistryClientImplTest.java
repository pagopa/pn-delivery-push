package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.InfoPaApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.SendIoMessageApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class PnExternalRegistryClientImplTest {
    
    @Mock
    private SendIoMessageApi sendIoMessageApi;

    @Mock
    private RootSenderIdApi rootSenderIdApi;

    @Mock
    private InfoPaApi infoPaApi;
    private PnExternalRegistryClientImpl client;

    @BeforeEach
    void setup() {
        client = new PnExternalRegistryClientImpl(sendIoMessageApi,rootSenderIdApi, infoPaApi);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void sendIOMessage() {

        SendMessageRequest request = new SendMessageRequest();
        request.setIun("001");

        SendMessageResponse response = new SendMessageResponse();
        response.setId("001");
        
        Mockito.when(sendIoMessageApi.sendIOMessageWithHttpInfo(request)).thenReturn(ResponseEntity.ok(response));

        SendMessageResponse resp = client.sendIOMessage(request);

        Assertions.assertEquals("001", resp.getId());
    }

}