package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class F24ApiReactiveConfiguration extends CommonBaseClient {

    @Bean
    public F24ControllerApi f24ReactiveServiceApi(PnDeliveryPushConfigs cfg){
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getF24BaseUrl() );
        return new F24ControllerApi(newApiClient);
    }
}
