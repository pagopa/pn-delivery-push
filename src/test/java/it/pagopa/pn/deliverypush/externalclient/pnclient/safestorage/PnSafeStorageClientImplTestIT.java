package it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.*;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClientImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class PnSafeStorageClientImplTestIT {

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryPushConfigs cfg;

    private PnSafeStorageClientImpl safeStorageClient;

    @BeforeEach
    void setup() {
        this.cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getSafeStorageBaseUrl()).thenReturn("http://localhost:8080");
        this.safeStorageClient = new PnSafeStorageClientImpl(restTemplate, cfg);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getFile() {
        //Given
        String fileKey = "abcd";
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("checksum");
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setDocumentType("PN_AAR");
        fileDownloadResponse.setDocumentStatus("SAVED");
        fileDownloadResponse.setKey(fileKey);
        fileDownloadResponse.setVersionId("v1");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        ResponseEntity<FileDownloadResponse> response = ResponseEntity.ok(fileDownloadResponse);


        //When
        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("pn-delivery-002");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);
        FileDownloadResponse result = safeStorageClient.getFile(fileKey, false);

        //Then
        Assertions.assertNotNull(result);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void createFile() {
        //Given
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

        ResponseEntity<FileCreationResponse> response = ResponseEntity.ok(fileCreationResponse);
        ResponseEntity<String> resp1 = ResponseEntity.ok("");

        Mockito.when(cfg.getSafeStorageCxId()).thenReturn("pn-delivery-002");

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withHeader("x-amz-meta-secret", fileCreationResponse.getSecret())
                        .withHeader("x-amz-checksum-sha256")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200));

        FileCreationResponse result = safeStorageClient.createFile(fileCreationRequest, "sha");

        //Then
        Assertions.assertNotNull(result);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void uploadContent() {
        //Given
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

        ResponseEntity<FileCreationResponse> response = ResponseEntity.ok(fileCreationResponse);
        ResponseEntity<String> resp1 = ResponseEntity.ok("");

        Mockito.when(restTemplate.exchange(Mockito.any(URI.class), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.eq(String.class)))
                .thenReturn(resp1);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withHeader("x-amz-meta-secret", fileCreationResponse.getSecret())
                        .withHeader("x-amz-checksum-sha256")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200));
        
        safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, "sha");
        Assertions.assertDoesNotThrow(() -> safeStorageClient.uploadContent (fileCreationRequest, fileCreationResponse,"sha"));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateFileMetadata() {
        //Given
        String fileKey = "abcd";
        String path = "/safe-storage/v1/files/" + fileKey;
        UpdateFileMetadataRequest updateFileMetadataRequest = new UpdateFileMetadataRequest();
        updateFileMetadataRequest.setStatus("ATTACHED");

        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");

        ResponseEntity<OperationResultCodeResponse> response = ResponseEntity.ok( operationResultCodeResponse);
        ResponseEntity<String> resp1 = ResponseEntity.ok("");

        Mockito.when( restTemplate.exchange( Mockito.any(), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        Mockito.when( cfg.getSafeStorageCxIdUpdatemetadata() ).thenReturn( "pn-delivery-002" );

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200));

        OperationResultCodeResponse result =  safeStorageClient.updateFileMetadata (fileKey, updateFileMetadataRequest);
        Assertions.assertEquals(operationResultCodeResponse.getResultCode(), result.getResultCode());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void updateFileMetadataKO() {
        //Given
        String fileKey = "abcd";
        String path = "/safe-storage/v1/files/" + fileKey;
        UpdateFileMetadataRequest updateFileMetadataRequest = new UpdateFileMetadataRequest();
        updateFileMetadataRequest.setStatus("ATTACHED");

        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("400.00");
        operationResultCodeResponse.setResultDescription("BAD SOMETHING");

        ResponseEntity<OperationResultCodeResponse> response = ResponseEntity.ok( operationResultCodeResponse);
        ResponseEntity<String> resp1 = ResponseEntity.ok("");

        Mockito.when( restTemplate.exchange( Mockito.any(), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        Mockito.when( cfg.getSafeStorageCxIdUpdatemetadata() ).thenReturn( "pn-delivery-002" );

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200));

        OperationResultCodeResponse result = safeStorageClient.updateFileMetadata (fileKey, updateFileMetadataRequest);

        Assertions.assertEquals(operationResultCodeResponse.getResultCode(), result.getResultCode());
    }
}
