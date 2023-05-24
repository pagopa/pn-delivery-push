package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery_reactive.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery_reactive.api.InternalOnlyApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public InternalOnlyApi internalOnlyApiReactive(PnDeliveryPushConfigs cfg){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getDeliveryBaseUrl());
        return new InternalOnlyApi(apiClient);
    }
}
