package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.ApiClient;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.api.NotificationsApi;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class PnDataVaultClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private NotificationsApi pnDataVaultNotificationApi;

    private PnDataVaultClientImpl client;

    @BeforeEach
    void setup() {
        cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getDataVaultBaseUrl()).thenReturn("http://localhost:8080");
        Mockito.when(cfg.getExternalchannelCxId()).thenReturn("pn-delivery-002");

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getDataVaultBaseUrl());

        pnDataVaultNotificationApi = new NotificationsApi(apiClient);
        client = new PnDataVaultClientImpl(restTemplate, cfg);
    }

    @Test
    void updateNotificationTimelineByIunAndTimelineElementId() {
        ConfidentialTimelineElementDto dto = buildConfidentialTimelineElementDto();

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        assertDoesNotThrow(() -> client.updateNotificationTimelineByIunAndTimelineElementId("01", dto));
    }

    @Test
    void getNotificationTimelineByIunAndTimelineElementId() {
        ConfidentialTimelineElementDto dto = buildConfidentialTimelineElementDto();

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
        Mockito.when(pnDataVaultNotificationApi.getNotificationTimelineByIunAndTimelineElementIdWithHttpInfo("001", "001")).thenReturn(ResponseEntity.ok(dto));

        ResponseEntity<ConfidentialTimelineElementDto> resp = client.getNotificationTimelineByIunAndTimelineElementId("001", "001");

        Assertions.assertEquals(resp, ResponseEntity.ok(dto));
    }

    @Test
    void getNotificationTimelineByIunWithHttpInfo() {
        ConfidentialTimelineElementDto dto = buildConfidentialTimelineElementDto();
        List<ConfidentialTimelineElementDto> dtoList = new ArrayList<>();
        dtoList.add(dto);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
        Mockito.when(pnDataVaultNotificationApi.getNotificationTimelineByIunWithHttpInfo("001")).thenReturn(ResponseEntity.ok(dtoList));

        ResponseEntity<List<ConfidentialTimelineElementDto>> resp = client.getNotificationTimelineByIunWithHttpInfo("001");

        Assertions.assertEquals(resp, ResponseEntity.ok(dtoList));
    }

    private ConfidentialTimelineElementDto buildConfidentialTimelineElementDto() {
        return ConfidentialTimelineElementDto.builder()
                .timelineElementId("001")
                .digitalAddress(
                        AddressDto.builder()
                                .value("indirizzo@test.com")
                                .build()
                )
                .build();
    }
}