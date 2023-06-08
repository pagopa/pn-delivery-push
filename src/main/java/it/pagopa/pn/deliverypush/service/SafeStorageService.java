package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import reactor.core.publisher.Mono;

public interface SafeStorageService {
    Mono<FileDownloadResponseInt> getFile(String fileKey, Boolean metadataOnly) ;
    
    Mono<FileCreationResponseInt> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);

    Mono<UpdateFileMetadataResponseInt> updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest);

    Mono<byte[]> downloadPieceOfContent(String fileKey, String url, long maxSize);
}
