package it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@SpringBootTest
@ActiveProfiles("test")
class PnSafeStorageClientImplTest {



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
        this.cfg = mock( PnDeliveryPushConfigs.class );
        Mockito.when( cfg.getSafeStorageBaseUrl() ).thenReturn( "http://localhost:8080" );
        Mockito.when( cfg.getSafeStorageCxId() ).thenReturn( "pn-delivery-002" );
        this.safeStorageClient = new PnSafeStorageClientImpl( restTemplate, cfg );
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
        ResponseEntity<FileDownloadResponse> response = ResponseEntity.ok( fileDownloadResponse);


        //When
        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( response );
        FileDownloadResponse result = safeStorageClient.getFile( fileKey, false );

        //Then
        Assertions.assertNotNull( result );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void createAndUploadContent() throws IOException {
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

        ResponseEntity<FileCreationResponse> response = ResponseEntity.ok( fileCreationResponse);

        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( response );

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                        .withHeader("x-amz-meta-secret", fileCreationResponse.getSecret())
                        .withHeader("x-amz-checksum-sha256")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200));

        FileCreationResponse result = safeStorageClient.createAndUploadContent (fileCreationRequest);

        //Then
        Assertions.assertNotNull( result );
    }
}