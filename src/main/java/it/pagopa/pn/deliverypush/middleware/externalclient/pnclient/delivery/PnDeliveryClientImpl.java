package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class PnDeliveryClientImpl implements PnDeliveryClient{
    private final InternalOnlyApi pnDeliveryApi;

    public PnDeliveryClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getDeliveryBaseUrl());
        this.pnDeliveryApi = new InternalOnlyApi( newApiClient );
    }
    
    @Override
    public void updateStatus(RequestUpdateStatusDto dto) {
        log.debug("Start updateState for iun={}", dto.getIun());

        pnDeliveryApi.updateStatusWithHttpInfo(dto);
        
        log.debug("Response update state OK for iun {}", dto.getIun());

    }

    @Override
    public SentNotification getSentNotification(String iun) {
        log.debug("Start getNotificationInfo for iun={}", iun);
        
        ResponseEntity<SentNotification> res = pnDeliveryApi.getSentNotificationPrivateWithHttpInfo(iun);
        
        log.debug("Response getNotificationInfo OK for iun {}", iun);
        
        return res.getBody();
    }
    
    @Override
    public Map<String, String>  getQuickAccessLinkTokensPrivate(String iun) {
        log.debug("Start getQuickAccessLinkTokensPrivate for iun={}", iun);

        ResponseEntity<Map<String, String>> res = pnDeliveryApi.getQuickAccessLinkTokensPrivateWithHttpInfo(iun);
        log.debug("Response getQuickAccessLinkTokensPrivate res={} for iun={}", res, iun);

        return res.getBody();
    }
}
