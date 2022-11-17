package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.model.SentNotification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.delivery-base-url=http://localhost:9998",
})
class PnDeliveryClientReactiveImplTestIT {
    @Autowired
    private PnDeliveryClientReactive client;
    
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
    @ExtendWith(SpringExtension.class)
    void getSentNotification() throws JsonProcessingException {
        //Given
        String iun ="iunTest";
        SentNotification notification = new SentNotification();
        notification.setIun(iun);
        
        String path = "/delivery-private/notifications/{iun}"
                .replace("{iun}",iun);
        
        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(notification);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));
        
        Mono<SentNotification> response = client.getSentNotification(iun);

        SentNotification notificationResponse = response.block();
        Assertions.assertNotNull(notificationResponse);
        Assertions.assertEquals(notification, notificationResponse);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getSentNotificationError(){
        //Given
        String iun ="iunTest";
        SentNotification notification = new SentNotification();
        notification.setIun(iun);

        String path = "/delivery-private/notifications/{iun}"
                .replace("{iun}",iun);
        
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400));

        client.getSentNotification(iun).onErrorResume(
                ex -> {
                    Assertions.assertNotNull(ex);
                    return Mono.empty();
                }
        );
    }
}