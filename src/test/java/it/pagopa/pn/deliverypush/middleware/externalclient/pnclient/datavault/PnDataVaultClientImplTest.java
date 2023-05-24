package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.api.NotificationsApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.AddressDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
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
        client = new PnDataVaultClientImpl(pnDataVaultNotificationApi);
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

        ConfidentialTimelineElementDto resp = client.getNotificationTimelineByIunAndTimelineElementId("001", "001");

        Assertions.assertEquals(resp, dto);
    }

    @Test
    void getNotificationTimelineByIunWithHttpInfo() {
        ConfidentialTimelineElementDto dto = buildConfidentialTimelineElementDto();
        List<ConfidentialTimelineElementDto> dtoList = new ArrayList<>();
        dtoList.add(dto);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
        Mockito.when(pnDataVaultNotificationApi.getNotificationTimelineByIunWithHttpInfo("001")).thenReturn(ResponseEntity.ok(dtoList));

        List<ConfidentialTimelineElementDto> resp = client.getNotificationTimelineByIunWithHttpInfo("001");

        Assertions.assertEquals(resp, dtoList);
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