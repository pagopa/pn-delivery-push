package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

public interface PnSafeStorageClient {
    String SAFE_STORAGE_URL_PREFIX = "safestorage://";

    String SAFE_STORAGE_DOCUMENT_TYPE_AAR = "PN_AAR";

    String SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT = "PN_LEGAL_FACTS";
    
    Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly);

    Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256);

    Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request);

    void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256);

}
