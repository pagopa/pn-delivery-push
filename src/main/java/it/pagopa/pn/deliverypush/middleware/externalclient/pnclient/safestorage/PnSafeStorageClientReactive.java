package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import reactor.core.publisher.Mono;

public interface PnSafeStorageClientReactive {
    Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly);
}
