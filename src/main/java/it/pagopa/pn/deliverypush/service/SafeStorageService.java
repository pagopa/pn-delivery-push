package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import reactor.core.publisher.Mono;

public interface SafeStorageService {

    FileDownloadResponseInt getFile(String fileKey, Boolean metadataOnly) ;
    
    Mono<FileDownloadResponseInt> getFileReactive(String fileKey, Boolean metadataOnly) ;

    FileCreationResponseInt createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);

    UpdateFileMetadataResponseInt updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest);
}
