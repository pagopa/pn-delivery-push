package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.emd.integration.api.MessageApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmdIntegrationConfigurator extends CommonBaseClient {

    @Bean
    public MessageApi messageApi(PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(cfg.getEmdIntegrationBaseUrl());
        return new MessageApi(newApiClient);
    }


}
