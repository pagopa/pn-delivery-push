package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class PnDeliveryClientImplTestIT {
    @Mock
    private RestTemplate restTemplate;

    private PnDeliveryPushConfigs cfg;
    
    private PnDeliveryClientImpl pnDeliveryClient;

    @BeforeEach
    public void setup() {
        cfg = Mockito.mock( PnDeliveryPushConfigs.class );
        pnDeliveryClient = new PnDeliveryClientImpl(restTemplate, cfg);
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void updateState() {
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun("iun")
                .build();
        ResponseUpdateStatusDto res = ResponseUpdateStatusDto.builder()
                .currentStatus(NotificationStatus.DELIVERING)
                .nextStatus(NotificationStatus.DELIVERED)
                .build();
        ResponseEntity<ResponseUpdateStatusDto> responseMock = ResponseEntity.ok(res);
        
        Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.<Class<ResponseUpdateStatusDto>>any())).thenReturn(responseMock);
                
        ResponseEntity<ResponseUpdateStatusDto> response = pnDeliveryClient.updateState(dto);

        Assertions.assertEquals(response.getBody(),res);
    }
}
