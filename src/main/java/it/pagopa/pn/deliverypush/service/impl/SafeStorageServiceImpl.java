package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class SafeStorageServiceImpl implements SafeStorageService {
    private final PnSafeStorageClient safeStorageClient;

    public SafeStorageServiceImpl(PnSafeStorageClient safeStorageClient) {
        this.safeStorageClient = safeStorageClient;
    }

    @Override
    public FileDownloadResponseInt getFile(String fileKey, Boolean metadataOnly) {
        FileDownloadResponse fileDownloadResponse = safeStorageClient.getFile(fileKey, metadataOnly);

        return getFileDownloadResponseInt(fileDownloadResponse);
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
    public FileCreationResponseInt createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        log.debug("Start call createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

        String sha256 = computeSha256(fileCreationRequest.getContent());

        FileCreationResponse fileCreationResponse = safeStorageClient.createFile(fileCreationRequest, sha256);

        safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256);

        FileCreationResponseInt fileCreationResponseInt = FileCreationResponseInt.builder()
                .key(fileCreationResponse.getKey())
                .build();
        
        log.info("createAndUploadContent file uploaded successfully key={} sha256={}", fileCreationResponseInt.getKey(), sha256);
        
        return fileCreationResponseInt;
    }


    @Override
    public UpdateFileMetadataResponseInt updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest) {
        log.debug("Start call updateFileMetadata - fileKey={} updateFileMetadataRequest={}", fileKey, updateFileMetadataRequest);

        var res = safeStorageClient.updateFileMetadata(fileKey, updateFileMetadataRequest);

        UpdateFileMetadataResponseInt updateFileMetadataResponseInt = UpdateFileMetadataResponseInt.builder()
                .resultCode(res.getResultCode())
                .errorList(res.getErrorList())
                .resultDescription(res.getResultDescription())
                .build();

        log.info("updateFileMetadata file endend key={} updateFileMetadataResponseInt={}", fileKey, updateFileMetadataRequest);

        return updateFileMetadataResponseInt;
    }

    
    private String computeSha256( byte[] content ) {

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest( content );
            return bytesToBase64( encodedhash );
        } catch (NoSuchAlgorithmException exc) {
            throw new PnInternalException("cannot compute sha256", exc );
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString( hash );
    }
}
