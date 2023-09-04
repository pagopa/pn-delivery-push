package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.auth.In;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
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

import java.time.Instant;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.delivery-base-url=http://localhost:9998",
})
class PnDeliveryClientReactiveImplTestIT extends MockAWSObjectsTest {
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

    @Test
    void removeAllNotificationCostsByIun() throws JsonProcessingException {
        //Given
        String iun ="iunTest";
        SentNotification notification = new SentNotification();
        notification.setIun(iun);

        String path = "/delivery-private/notification-cost/{iun}"
                .replace("{iun}",iun);

        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(notification);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("DELETE")
                        .withPath(path))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        Mono<Void> response = client.removeAllNotificationCostsByIun(iun);

        Assertions.assertDoesNotThrow(() ->  response.block());
    }

    @Test
    void removeAllNotificationCostsByIunError(){
        //Given
        String iun ="iunTest";
        SentNotification notification = new SentNotification();
        notification.setIun(iun);

        String path = "/delivery-private/notification-cost/{iun}"
                .replace("{iun}",iun);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("DELETE")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400));

        client.removeAllNotificationCostsByIun(iun).onErrorResume(
                ex -> {
                    Assertions.assertNotNull(ex);
                    return Mono.empty();
                }
        );
    }



    @Test
    void updateStatus() throws JsonProcessingException {
        //Given
        String iun ="iunTest";
        SentNotification notification = new SentNotification();
        notification.setIun(iun);

        String path = "/delivery-private/notifications/update-status"
                .replace("{iun}",iun);

        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(notification);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        NotificationStatusInt notificationStatus = NotificationStatusInt.CANCELLED;
        Instant updateStatus = Instant.now();

        Mono<Void> response = client.updateStatus (iun, notificationStatus, updateStatus);

        Assertions.assertDoesNotThrow(() ->  response.block());
    }

    @Test
    void updateStatusError(){
        //Given
        String iun ="iunTest";
        SentNotification notification = new SentNotification();
        notification.setIun(iun);

        String path = "/delivery-private/notifications/update-status"
                .replace("{iun}",iun);

        ObjectMapper mapper = new ObjectMapper();

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400));


        NotificationStatusInt notificationStatus = NotificationStatusInt.CANCELLED;
        Instant updateStatus = Instant.now();

        client.updateStatus (iun, notificationStatus, updateStatus).onErrorResume(
                ex -> {
                    Assertions.assertNotNull(ex);
                    return Mono.empty();
                }
        );
    }
}