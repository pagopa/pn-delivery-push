package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery_reactive.api.InternalOnlyApi;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnDeliveryClientReactiveImpl extends CommonBaseClient implements PnDeliveryClientReactive{
    private final InternalOnlyApi pnDeliveryApi;
    
    @Override
    public Mono<SentNotification> getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);
        
        return pnDeliveryApi.getSentNotificationPrivate(iun)
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, GET_NOTIFICATION));
    }


    @Override
    public Mono<Void> updateStatus(String iun, NotificationStatusInt notificationStatusInt, Instant updateStatusTimestamp) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_STATUS_NOTIFICATION);

        RequestUpdateStatusDto requestUpdateStatusDto = new RequestUpdateStatusDto();
        requestUpdateStatusDto.setIun(iun);
        requestUpdateStatusDto.setNextStatus(NotificationStatus.fromValue(notificationStatusInt.getValue()));
        requestUpdateStatusDto.setTimestamp(updateStatusTimestamp);


        return pnDeliveryApi.updateStatus(requestUpdateStatusDto)
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, UPDATE_STATUS_NOTIFICATION));
    }

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, REMOVE_IUV);

        return pnDeliveryApi.removeAllNotificationCostsByIun(iun)
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, REMOVE_IUV));
    }

}
