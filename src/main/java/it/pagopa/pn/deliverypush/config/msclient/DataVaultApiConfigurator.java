package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.api.NotificationsApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DataVaultApiConfigurator {
    @Bean
    @Primary
    public NotificationsApi notificationsApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg){
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getDataVaultBaseUrl());
        return new NotificationsApi( newApiClient );
    }
}
