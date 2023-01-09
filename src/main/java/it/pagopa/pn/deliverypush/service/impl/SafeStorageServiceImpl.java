package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.*;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Slf4j
@Service
public class SafeStorageServiceImpl implements SafeStorageService {
    private final PnSafeStorageClient safeStorageClient;
    private final PnSafeStorageClient safeStorageClientReactive;

    public SafeStorageServiceImpl(PnSafeStorageClient safeStorageClient, 
                                  PnSafeStorageClient safeStorageClientReactive) {
        this.safeStorageClient = safeStorageClient;
        this.safeStorageClientReactive = safeStorageClientReactive;
    }

    @Override
    public Mono<FileDownloadResponseInt> getFile(String fileKey, Boolean metadataOnly) {
        try {
            return safeStorageClient.getFile(fileKey, metadataOnly)
                    .map(this::getFileDownloadResponseInt);
        } catch ( PnInternalException ex ) {
            String description = String.format("Get file failed for fileKey=%s", fileKey);
            throw new PnNotFoundException("Not found", description, ERROR_CODE_DELIVERYPUSH_NOTFOUND, ex);
        }
    }

    private FileDownloadResponseInt getFileDownloadResponseInt(FileDownloadResponse fileDownloadResponse) {
        FileDownloadResponseInt.FileDownloadResponseIntBuilder responseIntBuilder = FileDownloadResponseInt.builder()
                .contentLength(fileDownloadResponse.getContentLength())
                .checksum(fileDownloadResponse.getChecksum())
                .contentType(fileDownloadResponse.getContentType())
                .key(fileDownloadResponse.getKey());

        if(fileDownloadResponse.getDownload() != null){
            responseIntBuilder.download(
                    FileDownloadInfoInt.builder()
                            .retryAfter(fileDownloadResponse.getDownload().getRetryAfter())
                            .url(fileDownloadResponse.getDownload().getUrl())
                            .build()
            );
        }

        return responseIntBuilder.build();
    }
    
    @Override
    public Mono<FileDownloadResponseInt> getFileReactive(String fileKey, Boolean metadataOnly){
        return safeStorageClientReactive.getFile(fileKey, metadataOnly)
                .onErrorResume( ex -> {
                            String message = String.format("Get file failed for - fileKey=%s isMetadataOnly=%b", fileKey, metadataOnly);
                            throw new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND, ex);
                        }
                )
                .doOnNext(fileDownloadResponse -> log.info("Response from SafeStorage: {}", fileDownloadResponse))
                .map(this::getFileDownloadResponseInt);
    }

    @Override
    public Mono<FileCreationResponseInt> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
            log.debug("Start call createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

            String sha256 = computeSha256(fileCreationRequest.getContent());

            return safeStorageClientReactive.createFile(fileCreationRequest, sha256)
                    .doOnNext(
                            fileCreationResponse -> safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256)
                    ).map(
                            fileCreationResponse ->{
                                FileCreationResponseInt fileCreationResponseInt = FileCreationResponseInt.builder()
                                        .key(fileCreationResponse.getKey())
                                        .build();

                                log.info("createAndUploadContent file uploaded successfully key={} sha256={}", fileCreationResponseInt.getKey(), sha256);
                                
                                return fileCreationResponseInt;
                            }

                    ).onErrorResume( Exception.class, exception ->{
                                throw new PnInternalException("Cannot createfile", ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR, exception);
                            }
                    );
    }


    @Override
    public Mono<UpdateFileMetadataResponseInt> updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest) {
        log.debug("Start call updateFileMetadata - fileKey={} updateFileMetadataRequest={}", fileKey, updateFileMetadataRequest);

        return safeStorageClient.updateFileMetadata(fileKey, updateFileMetadataRequest)
                .doOnSuccess( res ->
                        log.info("updateFileMetadata file endend key={} updateFileMetadataResponseInt={}", fileKey, updateFileMetadataRequest)
                )
                .doOnError( err -> {
                    throw new PnInternalException("Cannot updatemetadata", ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err);
                })
                .map( res -> UpdateFileMetadataResponseInt.builder()
                            .resultCode(res.getResultCode())
                            .errorList(res.getErrorList())
                            .resultDescription(res.getResultDescription())
                            .build()
                );
    }

    
    private String computeSha256( byte[] content ) {

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest( content );
            return bytesToBase64( encodedhash );
        } catch (Exception exc) {
            throw new PnInternalException("cannot compute sha256", ERROR_CODE_DELIVERYPUSH_ERRORCOMPUTECHECKSUM, exc );
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString( hash );
    }
}
