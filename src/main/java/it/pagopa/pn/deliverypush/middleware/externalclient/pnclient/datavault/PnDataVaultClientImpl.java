package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.api.NotificationsApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@CustomLog
@Component
@RequiredArgsConstructor
public class PnDataVaultClientImpl implements PnDataVaultClient{
    private final NotificationsApi pnDataVaultNotificationApi;
    
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
