package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.api.FileMetadataUpdateApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.api.FileUploadApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.net.URI;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GETFILEERROR;

@Component
@Slf4j
public class PnSafeStorageClientImpl extends CommonBaseClient implements PnSafeStorageClient {
    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final FileMetadataUpdateApi fileMetadataUpdateApi;
    private final RestTemplate restTemplate;

    private final PnDeliveryPushConfigs cfg;

    public PnSafeStorageClientImpl(PnDeliveryPushConfigs cfg,
                                   @Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate) {
        this.cfg = cfg;
        
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( this.cfg.getSafeStorageBaseUrl() );
        this.fileUploadApi =new FileUploadApi( newApiClient );
        this.fileDownloadApi = new FileDownloadApi( newApiClient );
        this.fileMetadataUpdateApi =new FileMetadataUpdateApi( newApiClient );
        this.restTemplate = restTemplate;
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly) {
        log.debug("Start call getFile - fileKey={} metadataOnly={}", fileKey, metadataOnly);
        fileKey = fileKey.replace(SAFE_STORAGE_URL_PREFIX, "");
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly )
                .onErrorResume( error ->{
                    log.error("Safe Storage client get file error ", error);
                    return Mono.error(new PnInternalException("Safe Storage client get file error", ERROR_CODE_DELIVERYPUSH_GETFILEERROR, error));
                });
    }

    @Override
    public Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        log.debug("Start call createFile - documentType={} filesize={} sha256={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length, sha256);

        return fileUploadApi.createFile( this.cfg.getSafeStorageCxId(),"SHA-256", sha256,  fileCreationRequest )
                .doOnSuccess( res ->  log.debug("File creation success - documentType={} filesize={} sha256={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length, sha256))
                .doOnError( res -> log.error("File creation error - documentType={} filesize={} sha256={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length, sha256));
    }

    @Override
    @Retryable(
            value = {PnInternalException.class},
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request) {
        log.debug("Start call updateFileMetadata - fileKey={} request={}", fileKey, request);

        return fileMetadataUpdateApi.updateFileMetadata( fileKey, this.cfg.getSafeStorageCxIdUpdatemetadata(), request )
                .doOnSuccess( res -> log.debug("End call updateFileMetadata, updated metadata file with key={}", fileKey))
                .onErrorResume( err -> {
                    log.error("Exception invoking updateFileMetadata fileKey={} err ",fileKey, err);
                    return Mono.error(new PnInternalException("Exception invoking updateFileMetadata", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                });
    }

    @Override
    public void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256) {
        try {
            log.info("Start upload content - key={} uploadUrl={}", fileCreationResponse.getKey(), fileCreationResponse.getUploadUrl());
            
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
            log.info("End upload content - key={} uploadUrl={}", fileCreationResponse.getKey(), fileCreationResponse.getUploadUrl());
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

}
