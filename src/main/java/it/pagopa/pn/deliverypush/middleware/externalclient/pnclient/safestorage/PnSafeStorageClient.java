package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

public interface PnSafeStorageClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE;
    String GET_FILE = "GET FILE";
    String CREATE_FILE = "FILE CREATION";
    String UPDATE_FILE_METADATA = "UPDATE FILE METADATA";
    String UPLOAD_FILE_CONTENT = "UPLOAD FILE CONTENT";

    String SAFE_STORAGE_URL_PREFIX = "safestorage://";

    String SAFE_STORAGE_DOCUMENT_TYPE_AAR = "PN_AAR";

    String SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT = "PN_LEGAL_FACTS";
    
    Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly);

    Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256);

    Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request);

    void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256);
 
}
