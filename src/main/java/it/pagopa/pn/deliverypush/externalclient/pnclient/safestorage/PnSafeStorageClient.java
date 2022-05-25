package it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;

public interface PnSafeStorageClient {


    FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) ;

    FileCreationResponse createFile(FileCreationRequest fileCreationRequest);

    FileCreationResponse createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);
}
