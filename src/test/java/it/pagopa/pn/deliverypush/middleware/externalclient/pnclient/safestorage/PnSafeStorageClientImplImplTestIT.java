package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.safe-storage-base-url=http://localhost:9998",
})
class PnSafeStorageClientImplImplTestIT extends MockAWSObjectsTest {
    @Autowired
    private PnSafeStorageClient client;

    @Mock
    private RestTemplate restTemplate;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void createFile() throws JsonProcessingException {
        //Given
        String fileKey ="fileKey";
        String path = "/safe-storage/v1/files";

        ObjectMapper mapper = new ObjectMapper();

        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setStatus("SAVED");
        fileCreationRequest.setDocumentType("PN_AAR");
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setContent(new byte[0]);

        String requestJson = mapper.writeValueAsString(fileCreationRequest);

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setSecret("secret");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.PUT);
        fileCreationResponse.setKey(fileKey);
        fileCreationResponse.setUploadUrl("http://localhost:9998" + path);

        String responseJson = mapper.writeValueAsString(fileCreationResponse);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                        .withBody(requestJson)
                )
                .respond(response()
                        .withBody(responseJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Mono<FileCreationResponse> responseMono = client.createFile(fileCreationRequest, "testSha");

        FileCreationResponse response = responseMono.block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(fileCreationResponse, response);
    }

    @Test
    void updateFileMetadata() throws JsonProcessingException {
        //Given
        String fileKey = "abcd";
        String path = "/safe-storage/v1/files/" + fileKey;

        ObjectMapper mapper = new ObjectMapper();

        UpdateFileMetadataRequest updateFileMetadataRequest = new UpdateFileMetadataRequest();
        updateFileMetadataRequest.setStatus("ATTACHED");


        String requestJson = mapper.writeValueAsString(updateFileMetadataRequest);

        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");

        String responseJson = mapper.writeValueAsString(operationResultCodeResponse);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                        .withBody(requestJson)
                )
                .respond(response()
                        .withBody(responseJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Mono<OperationResultCodeResponse> responseMono = client.updateFileMetadata(fileKey, updateFileMetadataRequest);

        OperationResultCodeResponse response = responseMono.block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(operationResultCodeResponse, response);
    }
    
    @Test
    void getFile() throws JsonProcessingException {
        //Given
        String fileKey ="fileKey2";

        FileDownloadResponse fileDownloadInput = new FileDownloadResponse();
        fileDownloadInput.setChecksum("checkSum")
        ;
        String path = "/safe-storage/v1/files/{fileKey}"
                .replace("{fileKey}", fileKey);

        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(fileDownloadInput);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter("metadataOnly", "true")
                )
                .respond(response()
                        .withBody(respJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Mono<FileDownloadResponse> response = client.getFile(fileKey, true);

        FileDownloadResponse fileDownloadResponse = response.block();
        Assertions.assertNotNull(fileDownloadResponse);
        Assertions.assertEquals(fileDownloadInput, fileDownloadResponse);
    }

    @Test
    void getFileError() throws JsonProcessingException {
        //Given
        String fileKey ="fileKey";

        FileDownloadResponse fileDownloadInput = new FileDownloadResponse();
        fileDownloadInput.setChecksum("checkSum")
        ;
        String path = "/safe-storage/v1/files/{fileKey}"
                .replace("{fileKey}", fileKey);

        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(fileDownloadInput);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter("metadataOnly", "true")
                )
                .respond(response()
                        .withBody(respJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(404)
                );
        Mono<FileDownloadResponse> fileDownloadResponseMono = client.getFile(fileKey, true);
        
        Assertions.assertThrows(RuntimeException.class, fileDownloadResponseMono::block);
    }
}