package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.api.NormalizeAddressServiceApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AddressManagerApiReactiveConfigurator extends CommonBaseClient {
    @Bean
    public NormalizeAddressServiceApi normalizeAddressReactiveServiceApi(PnDeliveryPushConfigs cfg){
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getAddressManagerBaseUrl() );
        return new NormalizeAddressServiceApi(newApiClient);
    }
}
