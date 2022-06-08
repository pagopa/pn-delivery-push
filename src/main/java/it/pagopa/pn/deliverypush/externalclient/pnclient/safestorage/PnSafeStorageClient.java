package it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;

public interface PnSafeStorageClient {

    String SAFE_STORAGE_URL_PREFIX = "safestorage://";

    public static final String SAFE_STORAGE_URL_PREFIX = "safestorage://";

    FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) ;

    FileCreationResponse createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);
}
