package it.pagopa.pn.deliverypush.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileDownloadApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileMetadataUpdateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileUploadApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafeStorageApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public FileUploadApi fileUploadApiReactive(PnDeliveryPushConfigs cfg){
        return new FileUploadApi( getNewApiClient(cfg) );
    }

    @Bean
    public FileDownloadApi fileDownloadApiReactive(PnDeliveryPushConfigs cfg){
        return new FileDownloadApi( getNewApiClient(cfg) );
    }

    @Bean
    public FileMetadataUpdateApi fileMetadataUpdateApiReactive(PnDeliveryPushConfigs cfg){
        return new FileMetadataUpdateApi( getNewApiClient(cfg) );
    }
    
    @NotNull
    private ApiClient getNewApiClient(PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );
        return newApiClient;
    }
}
