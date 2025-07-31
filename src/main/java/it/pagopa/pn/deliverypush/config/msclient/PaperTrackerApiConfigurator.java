package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.PaperStatusApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PaperTrackerApiConfigurator extends CommonBaseClient {

    @Bean
    @Primary
    public PaperStatusApi actionApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        return new PaperStatusApi(getNewApiClient(restTemplate, cfg));
    }

    @NotNull
    private ApiClient getNewApiClient( RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath( cfg.getPaperTrackerBaseUrl());
        return newApiClient;
    }
}
