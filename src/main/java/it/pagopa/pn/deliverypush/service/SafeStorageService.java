package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import reactor.core.publisher.Mono;

public interface SafeStorageService {
    String CREATE_FILE_PROCESS = "CREATE FILE";
    
    Mono<FileDownloadResponseInt> getFile(String fileKey, Boolean metadataOnly) ;
    
    Mono<FileCreationResponseInt> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);

    Mono<UpdateFileMetadataResponseInt> updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest);
}
