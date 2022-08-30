package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;

public interface PnSafeStorageClient {

    String SAFE_STORAGE_URL_PREFIX = "safestorage://";

    FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) ;

    FileCreationResponse createFile(FileCreationWithContentRequest fileCreationRequest, String sha256);

    void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256);
}
