package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.api.FileUploadApi;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

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
        Mockito.when(cfg.getExternalChannelBaseUrl()).thenReturn("http://localhost:8080");
        Mockito.when(cfg.getExternalchannelCxId()).thenReturn("pn-delivery-002");
        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("pn-delivery-002");
        
        client = new PnSafeStorageClientImpl(restTemplate, cfg);
    }

    @Test
    void getFile() {
        String fileKey = "abcd";
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("checksum");
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setDocumentType("PN_AAR");
        fileDownloadResponse.setDocumentStatus("SAVED");
        fileDownloadResponse.setKey(fileKey);
        fileDownloadResponse.setVersionId("v1");
        fileDownloadResponse.setDownload(new FileDownloadInfo());


        Mockito.when(fileDownloadApi.getFile(fileKey, "pn-delivery-002", Boolean.TRUE)).thenReturn(fileDownloadResponse);

        FileDownloadResponse response = client.getFile(fileKey, Boolean.TRUE);

        Assertions.assertEquals(fileDownloadResponse, response);
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
        
        Mockito.when(fileUploadApi.createFile(this.cfg.getSafeStorageCxId(), "SHA-256", sha256, fileCreationRequest)).thenReturn(fileCreationResponse);

        FileCreationResponse response = client.createFile(fileCreationRequest, sha256);

        Assertions.assertEquals(response, fileCreationResponse);
    }

    @Test
    void uploadContent() {
    }
}