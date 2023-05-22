package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.api.InternalOnlyApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@CustomLog
public class PnDeliveryClientReactiveImpl extends CommonBaseClient implements PnDeliveryClientReactive{
    private InternalOnlyApi pnDeliveryApi;
    private final PnDeliveryPushConfigs cfg;
    
    public PnDeliveryClientReactiveImpl(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getDeliveryBaseUrl());
        this.pnDeliveryApi = new InternalOnlyApi(apiClient);
    }
    
    @Override
    public Mono<SentNotification> getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);
        
        return pnDeliveryApi.getSentNotificationPrivate(iun)
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, GET_NOTIFICATION));
    }
}
