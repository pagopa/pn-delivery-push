package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.InfoPaApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.SendIoMessageApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ExternalRegistriesApiConfigurator {
    @Bean
    @Primary
    public SendIoMessageApi sendIoMessageApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg){
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getExternalRegistryBaseUrl());
        return new SendIoMessageApi( newApiClient );
    }

    @Bean
    @Primary
    public RootSenderIdApi rootSenderIdApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath( cfg.getExternalRegistryBaseUrl() );
        return new RootSenderIdApi(newApiClient);
    }

    @Bean
    @Primary
    public InfoPaApi infoPaApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath( cfg.getExternalRegistryBaseUrl() );
        return new InfoPaApi(newApiClient);
    }
}
