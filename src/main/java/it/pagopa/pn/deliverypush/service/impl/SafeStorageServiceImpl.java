package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.*;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClientReactive;
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
    private final PnSafeStorageClientReactive safeStorageClientReactive;

    public SafeStorageServiceImpl(PnSafeStorageClient safeStorageClient, 
                                  PnSafeStorageClientReactive safeStorageClientReactive) {
        this.safeStorageClient = safeStorageClient;
        this.safeStorageClientReactive = safeStorageClientReactive;
    }

    @Override
    public FileDownloadResponseInt getFile(String fileKey, Boolean metadataOnly) {
        try {
            FileDownloadResponse fileDownloadResponse = safeStorageClient.getFile(fileKey, metadataOnly);
            return getFileDownloadResponseInt( fileDownloadResponse );
        } catch ( PnInternalException ex ) {
            String message = String.format("Get file failed for - fileKey=%s isMetadataOnly=%b", fileKey, metadataOnly);
            throw new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND, ex);
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
    public FileCreationResponseInt createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        try {
            log.debug("Start call createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

            String sha256 = computeSha256(fileCreationRequest.getContent());

            FileCreationResponse fileCreationResponse = safeStorageClient.createFile(fileCreationRequest, sha256);

            safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256);

            FileCreationResponseInt fileCreationResponseInt = FileCreationResponseInt.builder()
                    .key(fileCreationResponse.getKey())
                    .build();

            log.info("createAndUploadContent file uploaded successfully key={} sha256={}", fileCreationResponseInt.getKey(), sha256);

            return fileCreationResponseInt;
        } catch (Exception e) {
            throw new PnInternalException("Cannot createfile", ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR, e);
        }
    }


    @Override
    public UpdateFileMetadataResponseInt updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest) {
        try {
            log.debug("Start call updateFileMetadata - fileKey={} updateFileMetadataRequest={}", fileKey, updateFileMetadataRequest);

            var res = safeStorageClient.updateFileMetadata(fileKey, updateFileMetadataRequest);

            UpdateFileMetadataResponseInt updateFileMetadataResponseInt = UpdateFileMetadataResponseInt.builder()
                    .resultCode(res.getResultCode())
                    .errorList(res.getErrorList())
                    .resultDescription(res.getResultDescription())
                    .build();

            log.info("updateFileMetadata file endend key={} updateFileMetadataResponseInt={}", fileKey, updateFileMetadataRequest);

            return updateFileMetadataResponseInt;
        } catch (Exception e) {
            throw new PnInternalException("Cannot updatemetadata", ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, e);
        }
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
