package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.api.UpdateNotificationCostApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalRegistriesApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public UpdateNotificationCostApi updateNotificationCostApi(PnDeliveryPushConfigs cfg){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getExternalRegistryBaseUrl());
        return new UpdateNotificationCostApi(apiClient);
    }
}
