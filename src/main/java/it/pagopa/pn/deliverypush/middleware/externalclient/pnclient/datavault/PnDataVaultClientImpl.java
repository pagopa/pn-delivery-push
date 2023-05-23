package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.api.NotificationsApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@CustomLog
@Component
public class PnDataVaultClientImpl implements PnDataVaultClient{
    private final NotificationsApi pnDataVaultNotificationApi;
    
    public PnDataVaultClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getDataVaultBaseUrl());
        this.pnDataVaultNotificationApi = new NotificationsApi( newApiClient );
    }
    
    public void updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto){
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_TIMELINE_ELEMENT_CONF_INFORMATION);

        pnDataVaultNotificationApi.updateNotificationTimelineByIunAndTimelineElementIdWithHttpInfo(iun, dto.getTimelineElementId(), dto);
    }

    public ConfidentialTimelineElementDto getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId){
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_CONF_INFORMATION);

        ResponseEntity<ConfidentialTimelineElementDto> resp = pnDataVaultNotificationApi.getNotificationTimelineByIunAndTimelineElementIdWithHttpInfo(iun, timelineElementId);
        
        return resp.getBody();
    }

    @Override
    public List<ConfidentialTimelineElementDto> getNotificationTimelineByIunWithHttpInfo(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_CONF_INFORMATION);

        ResponseEntity<List<ConfidentialTimelineElementDto>> resp = pnDataVaultNotificationApi.getNotificationTimelineByIunWithHttpInfo(iun);
        
        return resp.getBody();
    }

}
