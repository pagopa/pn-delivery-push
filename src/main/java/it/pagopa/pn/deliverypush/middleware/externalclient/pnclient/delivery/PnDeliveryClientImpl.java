package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.api.InternalOnlyApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@CustomLog
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
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_STATUS);
        pnDeliveryApi.updateStatusWithHttpInfo(dto);
    }

    @Override
    public SentNotification getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);

        ResponseEntity<SentNotification> res = pnDeliveryApi.getSentNotificationPrivateWithHttpInfo(iun);
        
        return res.getBody();
    }
    
    @Override
    public Map<String, String>  getQuickAccessLinkTokensPrivate(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_QUICK_ACCESS_TOKEN);
        ResponseEntity<Map<String, String>> res = pnDeliveryApi.getQuickAccessLinkTokensPrivateWithHttpInfo(iun);

        return res.getBody();
    }
}
