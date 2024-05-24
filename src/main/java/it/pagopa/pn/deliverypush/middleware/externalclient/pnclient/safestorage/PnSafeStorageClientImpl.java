package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.exceptions.PnFileGoneException;
import it.pagopa.pn.deliverypush.exceptions.PnFileNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileDownloadApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileMetadataUpdateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileUploadApi;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@CustomLog
public class PnSafeStorageClientImpl extends CommonBaseClient implements PnSafeStorageClient {
    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final FileMetadataUpdateApi fileMetadataUpdateApi;
    private final RestTemplate restTemplate;
    private final PnDeliveryPushConfigs cfg;

    public PnSafeStorageClientImpl(PnDeliveryPushConfigs cfg,
                                   @Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate,
                                   FileUploadApi fileUploadApi,
                                   FileDownloadApi fileDownloadApi,
                                   FileMetadataUpdateApi fileMetadataUpdateApi) {
        this.cfg = cfg;
        this.fileUploadApi = fileUploadApi;
        this.fileDownloadApi = fileDownloadApi;
        this.fileMetadataUpdateApi = fileMetadataUpdateApi;
        this.restTemplate = restTemplate;
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly) {
        log.logInvokingExternalService(CLIENT_NAME, GET_FILE);

        fileKey = fileKey.replace(SAFE_STORAGE_URL_PREFIX, "");
        String finalFileKey = fileKey;
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly )
                .doOnSuccess( res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, GET_FILE))
                .onErrorResume( WebClientResponseException.class, error ->{
                    log.error("Exception in call getFile fileKey={} error=", finalFileKey, error);

                    if(error.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                        log.error("File not found from safeStorage fileKey={} error={}", finalFileKey, error);
                        String errorDetail = "Allegato non trovato. fileKey=" + finalFileKey;
                        return Mono.error(
                                new PnFileNotFoundException(
                                        errorDetail,
                                        error      
                                )
                        );
                    } else if(error.getStatusCode().equals(HttpStatus.GONE)){
                        log.error("File deleted from safeStorage fileKey={} error={}", finalFileKey, error);
                        String errorDetail = "Allegato non disponibile: superati i termini di conservazione. fileKey=" + finalFileKey;
                        return Mono.error(
                            new PnFileGoneException(
                                errorDetail,
                                error
                            )
                        );
                    }
                    
                    return Mono.error(error);
                });
    }

    @Override
    public Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequestWithContent, String sha256) {
        log.logInvokingExternalService(CLIENT_NAME, CREATE_FILE);

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType(fileCreationRequestWithContent.getContentType());
        fileCreationRequest.setDocumentType(fileCreationRequestWithContent.getDocumentType());
        fileCreationRequest.setStatus(fileCreationRequestWithContent.getStatus());

        return fileUploadApi.createFile( this.cfg.getSafeStorageCxId(),"SHA-256", sha256,  fileCreationRequest )
                .doOnError( res -> log.error("File creation error - documentType={} filesize={} sha256={}", fileCreationRequest.getDocumentType(), fileCreationRequestWithContent.getContent().length, sha256));
    }

    @Override
    @Retryable(
            value = {PnInternalException.class},
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_FILE_METADATA);

        return fileMetadataUpdateApi.updateFileMetadata( fileKey, this.cfg.getSafeStorageCxIdUpdatemetadata(), request )
                .onErrorResume( err -> {
                    log.error("Exception invoking updateFileMetadata fileKey={} err ",fileKey, err);
                    return Mono.error(new PnInternalException("Exception invoking updateFileMetadata", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                });
    }

    @Override
    public void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256) {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, UPLOAD_FILE_CONTENT, fileCreationResponse.getKey());

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-type", fileCreationRequest.getContentType());
            headers.add("x-amz-checksum-sha256", sha256);
            headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

            HttpEntity<Resource> req = new HttpEntity<>(new ByteArrayResource(fileCreationRequest.getContent()), headers);

            URI url = URI.create(fileCreationResponse.getUploadUrl());
            HttpMethod method = fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;

            ResponseEntity<String> res = restTemplate.exchange(url, method, req, String.class);

            if (res.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value())
            {
                throw new PnInternalException("File upload failed", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR);
            }

        } catch (PnInternalException ee)
        {
            log.error("uploadContent PnInternalException uploading file", ee);
            throw ee;
        }
        catch (Exception ee)
        {
            log.error("uploadContent Exception uploading file", ee);
            throw new PnInternalException("Exception uploading file", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR, ee);
        }
    }

    /**
     * Scarica una parte (o tutto) il contenuto di un file
     * E' obbligatorio passare la dimensione richiesta, per rendere evidente che il metodo può essere usato per scaricare solo una parte di file
     *
     * @param url da scaricare
     * @param maxSize dimensione massima richiesta, -1 per scaricare tutto il file
     * @return array di dati
     */
    public byte[] downloadPieceOfContent(String url, long maxSize) {
        long readSize = 0;
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                readSize += bytesRead;
                if (maxSize > 0 && readSize > maxSize)
                    break;
            }
            return fileOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("Cannot read file content", e);
            throw new PnInternalException("cannot download content", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_READ_FILE_ERROR, e);
        }
    }

}
