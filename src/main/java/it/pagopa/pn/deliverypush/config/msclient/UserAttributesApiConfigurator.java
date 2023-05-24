package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.api.CourtesyApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.api.LegalApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class UserAttributesApiConfigurator {
    @Bean
    @Primary
    public CourtesyApi courtesyApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg){
        return new CourtesyApi( getNewApiClient(restTemplate, cfg) );
    }

    @Bean
    @Primary
    public LegalApi legalApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg){
        return new LegalApi( getNewApiClient(restTemplate, cfg) );
    }
    
    @NotNull
    private ApiClient getNewApiClient(RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getUserAttributesBaseUrl());
        return newApiClient;
    }

}
