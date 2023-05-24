package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NationalRegistriesApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public AgenziaEntrateApi agenziaEntrateApiReactive(PnDeliveryPushConfigs cfg){
        return new AgenziaEntrateApi(getNewApiClient(cfg));
    }

    @Bean
    public AddressApi addressApiReactive(PnDeliveryPushConfigs cfg){
        return new AddressApi(getNewApiClient(cfg));
    }
    
    @NotNull
    private ApiClient getNewApiClient(PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getNationalRegistriesBaseUrl() );
        return newApiClient;
    }
}
