package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TemplateApiConfiguration {

    @Bean
    @Primary
    public TemplateApi templateApiConfig(@Qualifier("withTracing") RestTemplate restTemplate,
                                         PnDeliveryPushConfigs cfg) {
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getTemplatesEngineBaseUrl());
        return new TemplateApi(apiClient);
    }

}
