package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileDownloadApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileMetadataUpdateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage_reactive.api.FileUploadApi;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


class PnSafeStorageClientImplTestRestTemplateIT {

    private static ClientAndServer mockServer;
//
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

    @Mock
    private FileDownloadApi fileDownloadApi;
    
    @Mock
    private FileUploadApi fileUploadApi;
    
    @Mock
    private FileMetadataUpdateApi fileMetadataUpdateApi;
    
    private PnSafeStorageClientImpl safeStorageClient;
    
    @BeforeEach
    void setup() {
        this.safeStorageClient = new PnSafeStorageClientImpl(cfg, restTemplate, fileUploadApi, fileDownloadApi, fileMetadataUpdateApi);
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
    
}
