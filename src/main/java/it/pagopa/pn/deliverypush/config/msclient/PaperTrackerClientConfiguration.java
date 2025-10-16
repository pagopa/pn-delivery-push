package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaperTrackerClientConfiguration extends CommonBaseClient {

        @Bean
        public NotificationReworkApi paperTrackerNotificationReworkApi(PnDeliveryPushConfigs cfg){
            ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
            newApiClient.setBasePath( cfg.getPaperTrackerClientBaseUrl() );
            return new NotificationReworkApi(newApiClient);
        }
}
