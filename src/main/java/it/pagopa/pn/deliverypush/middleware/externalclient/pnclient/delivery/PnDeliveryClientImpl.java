package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.api.InternalOnlyApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@CustomLog
@RequiredArgsConstructor
@Component
public class PnDeliveryClientImpl implements PnDeliveryClient{
    private final InternalOnlyApi pnDeliveryApi;
    
    @Override
    public void updateStatus(RequestUpdateStatusDto dto) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_STATUS);
        pnDeliveryApi.updateStatusWithHttpInfo(dto);
    }

    @Override
    public SentNotification getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);

    //    ResponseEntity<SentNotification> res = pnDeliveryApi.getSentNotificationPrivateWithHttpInfo(iun);

        SentNotification sentNotification = new SentNotification();
        sentNotification.setIun(iun);
        sentNotification.setSenderPaId("Milano1");
        
        
        return sentNotification;
    }
    
    @Override
    public Map<String, String>  getQuickAccessLinkTokensPrivate(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_QUICK_ACCESS_TOKEN);
        ResponseEntity<Map<String, String>> res = pnDeliveryApi.getQuickAccessLinkTokensPrivateWithHttpInfo(iun);

        return res.getBody();
    }
}
