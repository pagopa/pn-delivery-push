package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GETFILEERROR;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Component
@Slf4j
public class PnSafeStorageClientReactiveImpl extends BaseClient implements PnSafeStorageClientReactive{
    private final FileDownloadApi fileDownloadApi;
    private final PnDeliveryPushConfigs cfg;

    public PnSafeStorageClientReactiveImpl(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
        
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()).build() );
        newApiClient.setBasePath( this.cfg.getSafeStorageBaseUrl() );

        this.fileDownloadApi = new FileDownloadApi( newApiClient );
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly) {
        log.debug("Start call getFile - fileKey={} metadataOnly={}", fileKey, metadataOnly);
        fileKey = fileKey.replace(SAFE_STORAGE_URL_PREFIX, "");
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly )
                .onErrorResume( error -> {
                    throw new PnInternalException("Safe Storage client get file error", ERROR_CODE_DELIVERYPUSH_GETFILEERROR, error);
                });
    }
    
}
