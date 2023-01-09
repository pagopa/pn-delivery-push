package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileMetadataUpdateApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.*;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

class PnSafeStorageClientImplTest {

    private FileDownloadApi fileDownloadApi;

    private FileUploadApi fileUploadApi;

    private FileMetadataUpdateApi fileMetadataUpdateApi;

    private PnDeliveryPushConfigs cfg;

    private RestTemplate restTemplate;

    private PnSafeStorageClientImpl client;

    @BeforeEach
    void setUp() {
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        restTemplate = Mockito.mock(RestTemplate.class);

        Mockito.when(cfg.getSafeStorageBaseUrl()).thenReturn("http://localhost:8080");

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getSafeStorageBaseUrl());

        fileDownloadApi = new FileDownloadApi(apiClient);
        fileUploadApi = new FileUploadApi(apiClient);
        fileMetadataUpdateApi = new FileMetadataUpdateApi(apiClient);

        client = new PnSafeStorageClientImpl(cfg, restTemplate);
    }

    @Test
    void getFile() {
        FileDownloadResponse fileDownloadResponse = buildFileDownloadResponse();

        ResponseEntity<FileDownloadResponse> response = ResponseEntity.ok(fileDownloadResponse);

        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("pn-delivery-002");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        Mono<FileDownloadResponse> monoActual = client.getFile("fileKey", Boolean.TRUE);
        FileDownloadResponse actual = monoActual.block();
        
        Assertions.assertEquals(fileDownloadResponse, actual);
    }

    @Test
    void getFileError() {
        FileDownloadResponse fileDownloadResponse = buildFileDownloadResponse();

        ResponseEntity<FileDownloadResponse> response = ResponseEntity.ok(fileDownloadResponse);

        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("pn-delivery-002");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenThrow( new RestClientException("error") );
        
        Assertions.assertThrows( PnInternalException.class, () ->{
            client.getFile("fileKey", Boolean.TRUE);
        });
    }
    
    @Test
    void createFile() {
        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setSecret("secret");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.PUT);
        fileCreationResponse.setKey("fileKey");
        fileCreationResponse.setUploadUrl("http://localhost:9998");

        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setStatus("SAVED");
        fileCreationRequest.setDocumentType("PN_AAR");
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setContent(new byte[0]);

        ResponseEntity<FileCreationResponse> response = ResponseEntity.ok(fileCreationResponse);
        ResponseEntity<String> resp1 = ResponseEntity.ok("");

        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("pn-delivery-002");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        Mono<FileCreationResponse> monoActual = client.createFile(fileCreationRequest, "sha256");

        FileCreationResponse actual = monoActual.block();
        Assertions.assertEquals(fileCreationResponse, actual);
    }

    @Test
    void updateFileMetadata() {
        String fileKey = "abcd";
        String path = "/safe-storage/v1/files/" + fileKey;
        UpdateFileMetadataRequest updateFileMetadataRequest = new UpdateFileMetadataRequest();
        updateFileMetadataRequest.setStatus("ATTACHED");

        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");

        ResponseEntity<OperationResultCodeResponse> response = ResponseEntity.ok(operationResultCodeResponse);
        ResponseEntity<String> resp1 = ResponseEntity.ok("");

        Mockito.when(restTemplate.exchange(Mockito.any(), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        Mockito.when(cfg.getSafeStorageCxIdUpdatemetadata()).thenReturn("pn-delivery-002");

        Mono<OperationResultCodeResponse> monoActual = client.updateFileMetadata("fileKey", updateFileMetadataRequest);

        OperationResultCodeResponse actual = monoActual.block();
        Assertions.assertEquals(operationResultCodeResponse, actual);
    }

    private FileDownloadResponse buildFileDownloadResponse() {
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("checksum");
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setDocumentType("PN_AAR");
        fileDownloadResponse.setDocumentStatus("SAVED");
        fileDownloadResponse.setKey("fileKey");
        fileDownloadResponse.setVersionId("v1");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        return fileDownloadResponse;
    }

}