package it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage;

import com.amazonaws.util.Base64;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class PnSafeStorageClientImpl implements PnSafeStorageClient {

    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final PnDeliveryPushConfigs cfg;
    private final RestTemplate restTemplate;

    public PnSafeStorageClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient( restTemplate );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );

        this.fileDownloadApi = new FileDownloadApi( newApiClient );
        this.fileUploadApi =new FileUploadApi( newApiClient );
        this.restTemplate = restTemplate;
        this.cfg = cfg;
    }


    public FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) {
        log.debug("Start call getFile - fileKey {} metadataOnly {}", fileKey, metadataOnly);
        // elimino eventuale prefisso di safestorage
        fileKey = fileKey.replace(SAFE_STORAGE_URL_PREFIX, "");
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly );
    }

    @Override
    public FileCreationResponse createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
            log.debug("Start call createAndUploadFile - documentType {} filesize:{}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

            String sha256 = DigestUtils.sha256Hex(fileCreationRequest.getContent());

            FileCreationResponse fileCreationResponse = fileUploadApi.createFile( this.cfg.getSafeStorageCxId(), fileCreationRequest );

            log.debug("createAndUploadContent create file preloaded sha256:{}", sha256);

            this.uploadContent(fileCreationRequest, fileCreationResponse, sha256);

            log.info("createAndUploadContent file uploaded successfully key:{}", fileCreationResponse.getKey());

            return fileCreationResponse;
    }

    private void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256){

        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-type", fileCreationRequest.getContentType());
            headers.add("x-amz-checksum-sha256", Base64.encodeAsString(sha256.getBytes(StandardCharsets.UTF_8)));
            headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

            HttpEntity<Resource> req = new HttpEntity<>(new ByteArrayResource(fileCreationRequest.getContent()), headers);
            ResponseEntity<String> res = restTemplate.exchange( URI.create(fileCreationResponse.getUploadUrl()),
                    fileCreationResponse.getUploadMethod()== FileCreationResponse.UploadMethodEnum.POST? HttpMethod.POST:HttpMethod.PUT, req, String.class);
            if (res.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value())
            {
                throw new PnInternalException("File upload failed");
            }
        } catch (PnInternalException ee)
        {
            log.error("uploadContent PnInternalException uploading file", ee);
            throw ee;
        }
        catch (Exception ee)
        {
            log.error("uploadContent Exception uploading file", ee);
            throw new PnInternalException("Exception uploading file", ee);
        }
    }
}
