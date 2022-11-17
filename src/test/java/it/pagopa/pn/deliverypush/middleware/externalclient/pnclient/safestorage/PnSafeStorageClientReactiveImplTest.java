package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage_reactive.model.FileDownloadResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.safe-storage-base-url=http://localhost:9998",
})
class PnSafeStorageClientReactiveImplTest {
    @Autowired
    private PnSafeStorageClientReactive client;

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
    void getFile() throws JsonProcessingException {
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
                        .withStatusCode(200)
                );

        Mono<FileDownloadResponse> response = client.getFile(fileKey, true);

        FileDownloadResponse fileDownloadResponse = response.block();
        Assertions.assertNotNull(fileDownloadResponse);
        Assertions.assertEquals(fileDownloadInput, fileDownloadResponse);
    }
}