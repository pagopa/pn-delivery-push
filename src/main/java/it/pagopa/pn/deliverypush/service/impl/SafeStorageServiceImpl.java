package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Slf4j
@Service
public class SafeStorageServiceImpl implements SafeStorageService {
    private final PnSafeStorageClient safeStorageClient;

    public SafeStorageServiceImpl(PnSafeStorageClient safeStorageClient) {
        this.safeStorageClient = safeStorageClient;
    }

    @Override
    public Mono<FileDownloadResponseInt> getFile(String fileKey, Boolean metadataOnly) {
        MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, fileKey);

        return safeStorageClient.getFile(fileKey, metadataOnly)
                .doOnSuccess(fileDownloadResponse -> {
                    log.debug("Response getFile from SafeStorage: {}", fileDownloadResponse);
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                })
                .doOnError(err -> MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY))
                .map(this::getFileDownloadResponseInt);
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
    public Mono<FileCreationResponseInt> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        log.info("Start createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

        String sha256 = computeSha256(fileCreationRequest.getContent());

        return safeStorageClient.createFile(fileCreationRequest, sha256)
                .onErrorResume( Exception.class, exception ->{
                    log.error("Cannot create file ", exception);
                    return Mono.error(new PnInternalException("Cannot create file", ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR, exception));
                })
                .flatMap(fileCreationResponse -> 
                    Mono.fromRunnable(() -> safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256))
                            .thenReturn(fileCreationResponse)
                            .map(fileCreationResponse2 ->{
                                FileCreationResponseInt fileCreationResponseInt = FileCreationResponseInt.builder()
                                        .key(fileCreationResponse2.getKey())
                                        .build();

                                log.info("createAndUploadContent file uploaded successfully key={} sha256={}", fileCreationResponseInt.getKey(), sha256);

                                return fileCreationResponseInt;
                            })
                );
    }
    
    @Override
    public Mono<UpdateFileMetadataResponseInt> updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest) {
        MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, fileKey);
        log.debug("Start call updateFileMetadata - fileKey={} updateFileMetadataRequest={}", fileKey, updateFileMetadataRequest);

        return safeStorageClient.updateFileMetadata(fileKey, updateFileMetadataRequest)
                .doOnSuccess( res -> {
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                    log.debug("updateFileMetadata file ok key={} updateFileMetadataResponseInt={}", fileKey, updateFileMetadataRequest);
                })
                .onErrorResume( err ->{
                    log.error("Cannot update metadata ", err);
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                    return Mono.error(new PnInternalException("Cannot update metadata", ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                })
                .map( res -> UpdateFileMetadataResponseInt.builder()
                            .resultCode(res.getResultCode())
                            .errorList(res.getErrorList())
                            .resultDescription(res.getResultDescription())
                            .build()
                );
    }

    @Override
    public Mono<byte[]> downloadPieceOfContent(String fileKey, String url, long maxSize) {
        MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, fileKey);
        log.debug("Start call downloadPieceOfContent - fileKey={} url={} maxSize={}", fileKey, url, maxSize);

        return Mono.fromSupplier(() ->  safeStorageClient.downloadPieceOfContent(url, maxSize))
                .doOnSuccess( res -> {
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                    log.debug("downloadPieceOfContent file ok key={} url={} read size={}", fileKey, url, res==null?null:res.length);
                })
                .onErrorResume( err ->{
                    log.error("Cannot download content ", err);
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                    return Mono.error(new PnInternalException("Cannot update metadata", ERROR_CODE_DELIVERYPUSH_READ_FILE_ERROR, err));
                });
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
