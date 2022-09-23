package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.mockito.Mockito.mock;

class PnDeliveryClientImplTest {

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InternalOnlyApi pnDeliveryApi;

    private PnDeliveryClientImpl client;

    @BeforeEach
    void setup() {
        this.cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getDeliveryBaseUrl()).thenReturn("http://localhost:8080");

        restTemplate = Mockito.mock(RestTemplate.class);
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getDeliveryBaseUrl());
        client = new PnDeliveryClientImpl(restTemplate, cfg);
        pnDeliveryApi = new InternalOnlyApi(apiClient);
    }

    @Test
    void updateStatus() {

        RequestUpdateStatusDto statusDto = new RequestUpdateStatusDto();
        statusDto.setIun("001");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        ResponseEntity<Void> resp = client.updateStatus(statusDto);

        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

    }

    @Test
    void getSentNotification() {
        SentNotification notification = new SentNotification();
        notification.setIun("001");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
        
        Mockito.when(pnDeliveryApi.getSentNotificationPrivateWithHttpInfo("001")).thenReturn(ResponseEntity.ok(notification));

        ResponseEntity<SentNotification> res = client.getSentNotification("001");

        Assertions.assertEquals("001", res.getBody().getIun());

    }

    @Test
    void getNotificationCostPrivate() {

        NotificationCostResponse response = new NotificationCostResponse();
        response.setRecipientIdx(0);
        response.setIun("0");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(pnDeliveryApi.getNotificationCostPrivateWithHttpInfo("0", "0")).thenReturn(ResponseEntity.of(Optional.of(response)));

        ResponseEntity<NotificationCostResponse> res = client.getNotificationCostPrivate("0", "0");

        Assertions.assertEquals("0", res.getBody().getIun());
    }
}