package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.ApiClient;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.api.SendIoMessageApi;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.mockito.Mockito.mock;

class PnExternalRegistryClientImplTest {

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SendIoMessageApi sendIoMessageApi;

    private PnExternalRegistryClientImpl client;

    @BeforeEach
    void setup() {
        this.cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getExternalRegistryBaseUrl()).thenReturn("http://localhost:8080");

        restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when((restTemplate.getUriTemplateHandler())).thenReturn(new DefaultUriBuilderFactory());
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalRegistryBaseUrl());
        client = new PnExternalRegistryClientImpl(restTemplate, cfg);
        sendIoMessageApi = new SendIoMessageApi(apiClient);
    }

    @Test
    void sendIOMessage() {

        SendMessageRequest request = new SendMessageRequest();
        request.setIun("001");

        SendMessageResponse response = new SendMessageResponse();
        response.setId("001");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(sendIoMessageApi.sendIOMessageWithHttpInfo(request)).thenReturn(ResponseEntity.ok(response));

        SendMessageResponse resp = client.sendIOMessage(request);

        Assertions.assertEquals("001", resp.getId());
    }

}