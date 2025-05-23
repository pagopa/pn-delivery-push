package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV25;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery_reactive.api.InternalOnlyApi;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;


import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnDeliveryClientReactiveImpl extends CommonBaseClient implements PnDeliveryClientReactive{
    private final InternalOnlyApi pnDeliveryApi;
    
    @Override
    public Mono<SentNotificationV25> getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);
        
        return pnDeliveryApi.getSentNotificationPrivate(iun)
                .onErrorResume( error -> {
                    log.error("Get notification error ={} - iun {}", error,  iun);
                    if (error instanceof WebClientResponseException webClientResponseException)
                    {
                        if (webClientResponseException.getStatusCode() == HttpStatus.NOT_FOUND)
                        {
                            return Mono.error(new PnNotFoundException("Not found", "Get notification is not valid for - iun " + iun,
                                    ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED));
                        }
                    }
                    return Mono.error(new PnInternalException("Get notification error - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED, error));
                })
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, GET_NOTIFICATION));
    }

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, REMOVE_IUV);

        return pnDeliveryApi.removeAllNotificationCostsByIun(iun)
                .doOnSuccess(res -> log.debug("Received sync response from {} for {} ", CLIENT_NAME, REMOVE_IUV));
    }

}
