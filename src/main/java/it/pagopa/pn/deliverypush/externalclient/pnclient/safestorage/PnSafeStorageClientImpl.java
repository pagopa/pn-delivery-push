package it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;

@Slf4j
@Component
public class PnSafeStorageClientImpl implements PnSafeStorageClient {

    private final FileDownloadApi fileDownloadApi;
    private final FileUploadApi fileUploadApi;
    private final PnDeliveryPushConfigs cfg;

    public PnSafeStorageClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient( restTemplate );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );

        this.fileDownloadApi = new FileDownloadApi( newApiClient );
        this.fileUploadApi =new FileUploadApi( newApiClient );
        this.cfg = cfg;
    }


    public FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) {
        log.debug("Start call getFile - fileKey {} metadataOnly {}", fileKey, metadataOnly);
        return fileDownloadApi.getFile( fileKey, this.cfg.getSafeStorageCxId(), metadataOnly );
    }

    public FileCreationResponse createFile(FileCreationRequest fileCreationRequest) {
        log.debug("Start call createFile - fileCreationRequest {}", fileCreationRequest);
        return fileUploadApi.createFile( this.cfg.getSafeStorageCxId(), fileCreationRequest );
    }

    @Override
    public FileCreationResponse createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
            log.debug("Start call createAndUploadFile - fileCreationRequest {} filesize:{}", fileCreationRequest, fileCreationRequest.getContent().length);
            FileCreationResponse fileCreationResponse = this.createFile(fileCreationRequest);
            log.debug("createAndUploadContent create file preloaded");
            this.uploadContent(fileCreationRequest, fileCreationResponse);
            log.info("createAndUploadContent file uploaded successfully key:{}", fileCreationResponse.getKey());
            return fileCreationResponse;
    }

    private void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse){
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpEntityEnclosingRequestBase httppost;
            if (fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST)
                httppost = new HttpPost(fileCreationResponse.getUploadUrl());
            else
                httppost = new HttpPut(fileCreationResponse.getUploadUrl());

            final InputStreamEntity reqEntity = new InputStreamEntity(
                    new ByteArrayInputStream(fileCreationRequest.getContent()), -1, ContentType.parse(fileCreationRequest.getContentType()));
            httppost.setEntity(reqEntity);

            log.debug("uploadContent Executing request " + httppost.getMethod() + " " + httppost.getURI());
            try (final CloseableHttpResponse response = httpclient.execute(httppost)) {
                log.debug("uploadContent response: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                {
                    throw new PnInternalException("File upload failed");
                }
            }
        }
        catch (PnInternalException ee)
        {
            throw ee;
        }
        catch (Exception ee)
        {
            log.error("uploadContent Exception uploading file", ee);
            throw new PnInternalException("Exception uploading file", ee);
        }
    }
}
