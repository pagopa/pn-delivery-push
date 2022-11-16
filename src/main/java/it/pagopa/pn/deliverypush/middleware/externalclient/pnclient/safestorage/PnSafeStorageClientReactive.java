package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.model.FileDownloadResponse;
import reactor.core.publisher.Mono;

public interface PnSafeStorageClientReactive {
    Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly);
}
