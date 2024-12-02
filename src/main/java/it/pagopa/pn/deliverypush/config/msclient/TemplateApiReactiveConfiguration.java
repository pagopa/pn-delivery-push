package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine_reactive.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine_reactive.api.TemplateApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateApiReactiveConfiguration extends CommonBaseClient {

    @Bean
    public TemplateApi templateReactiveConfiguration(PnDeliveryPushConfigs cfg) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath("http://localhost:8099");
        return new TemplateApi(apiClient);
    }
}
