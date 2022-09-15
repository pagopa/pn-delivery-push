package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class PnSafeStorageClientImplTest {

    @Mock
    private FileDownloadApi fileDownloadApi;

    @Mock
    private FileUploadApi fileUploadApi;

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private RestTemplate restTemplate;

    private PnSafeStorageClientImpl client;

    @BeforeEach
    void setup() {
        this.cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getSafeStorageBaseUrl()).thenReturn("http://localhost:8080");
        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("test001");

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getSafeStorageBaseUrl());

        this.fileDownloadApi = new FileDownloadApi(apiClient);
        this.fileUploadApi = new FileUploadApi(apiClient);

        client = new PnSafeStorageClientImpl(restTemplate, cfg);
    }

    @Test
    void getFile() {
        String fileKey = "abcd";
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));
       
        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
       
//        Mockito.when(fileDownloadApi.getFile(fileKey, "pn-delivery-002", Boolean.FALSE)).thenReturn(fileDownloadResponse);
//
//        FileDownloadResponse response = client.getFile(fileKey, Boolean.TRUE);
//
//        Assertions.assertEquals(fileDownloadResponse, response);
    }

    @Test
    void createFile() {

        String fileKey = "abcd";
        String sha256 = "base64Sha256";
        String path = "/fileuploadid123123123";
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setStatus("SAVED");
        fileCreationRequest.setDocumentType("PN_AAR");
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setContent(new byte[0]);

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setSecret("secret");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.PUT);
        fileCreationResponse.setKey(fileKey);
        fileCreationResponse.setUploadUrl("http://localhost:9998" + path);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
//        Mockito.when(fileUploadApi.createFile(this.cfg.getSafeStorageCxId(), "SHA-256", sha256, fileCreationRequest)).thenReturn(fileCreationResponse);
//
//        FileCreationResponse response = client.createFile(fileCreationRequest, sha256);
//
//        Assertions.assertEquals(response, fileCreationResponse);
    }

    @Test
    void uploadContent() {
    }
}