package it.pagopa.pn.deliverypush.pnclient.delivery;

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
    public ResponseEntity<Void> updateStatus(RequestUpdateStatusDto dto) {
        log.debug("Start updateState for iun={}", dto.getIun());

        ResponseEntity<Void> resp = pnDeliveryApi.updateStatusWithHttpInfo(dto);
        log.debug("Response update state for iun {} is {}", dto.getIun(), resp);
        
        return resp;
    }

    @Override
    public ResponseEntity<SentNotification> getSentNotification(String iun) {
        log.debug("Start getNotificationInfo for iun={}", iun);
        
        ResponseEntity<SentNotification> res = pnDeliveryApi.getSentNotificationPrivateWithHttpInfo(iun);
        log.debug("Response getNotificationInfo for iun {} is {}", iun, res);
        
        return res;
    }
}
