package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.api.PaperMessagesApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PaperChannelApiConfigurator {
    @Bean
    @Primary
    public PaperMessagesApi paperMessagesApi(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate, PnDeliveryPushConfigs cfg){
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getPaperChannelBaseUrl());
        return new PaperMessagesApi(apiClient);
    }
}
