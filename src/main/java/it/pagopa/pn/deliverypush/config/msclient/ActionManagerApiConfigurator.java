package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.api.ActionApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ActionManagerApiConfigurator extends CommonBaseClient {

    @Bean
    @Primary
    public ActionApi actionApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        return new ActionApi(getNewApiClient(restTemplate, cfg));
    }

    @NotNull
    private ApiClient getNewApiClient( RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath( cfg.getActionManagerBaseUrl());
        return newApiClient;
    }
}
