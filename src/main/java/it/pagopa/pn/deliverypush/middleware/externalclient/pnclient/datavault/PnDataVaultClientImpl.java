package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.ApiClient;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.api.NotificationsApi;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class PnDataVaultClientImpl implements PnDataVaultClient{
    private final NotificationsApi pnDataVaultNotificationApi;

    public PnDataVaultClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getDataVaultBaseUrl());
        this.pnDataVaultNotificationApi = new NotificationsApi( newApiClient );
    }
    
    public ResponseEntity<Void> updateNotificationTimelineByIunAndTimelineElementId(String iun, ConfidentialTimelineElementDto dto){
        log.debug("Start call updateNotificationTimelineByIunAndTimelineElementId - iun={} timelineElementId={}", iun, dto.getTimelineElementId());

        ResponseEntity<Void> resp = pnDataVaultNotificationApi.updateNotificationTimelineByIunAndTimelineElementIdWithHttpInfo(iun, dto.getTimelineElementId(), dto);

        log.debug("Response updateNotificationTimelineByIunAndTimelineElementId - iun={} timelineElementId={}", iun, dto.getTimelineElementId());
        
        return resp;
    }

    public ResponseEntity<ConfidentialTimelineElementDto> getNotificationTimelineByIunAndTimelineElementId(String iun, String timelineElementId){
        log.debug("Start call getNotificationTimelineByIunAndTimelineElementId - iun={} timelineElementId={}", iun, timelineElementId);

        ResponseEntity<ConfidentialTimelineElementDto> resp = pnDataVaultNotificationApi.getNotificationTimelineByIunAndTimelineElementIdWithHttpInfo(iun, timelineElementId);

        log.debug("Response getNotificationTimelineByIunAndTimelineElementId - iun={} timelineElementId={}", iun, timelineElementId);

        return resp;
    }

    @Override
    public ResponseEntity<List<ConfidentialTimelineElementDto>> getNotificationTimelineByIunWithHttpInfo(String iun) {
        log.debug("Start call getNotificationTimelineByIunWithHttpInfo - iun={}", iun);

        ResponseEntity<List<ConfidentialTimelineElementDto>> resp = pnDataVaultNotificationApi.getNotificationTimelineByIunWithHttpInfo(iun);

        log.debug("Response getNotificationTimelineByIunWithHttpInfo - iun={}", iun);

        return resp;
    }

}
