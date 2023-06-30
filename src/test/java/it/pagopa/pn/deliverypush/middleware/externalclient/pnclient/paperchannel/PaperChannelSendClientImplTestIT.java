package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.ProductTypeEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.paper-channel-base-url=http://localhost:9998",
})
class PaperChannelSendClientImplTestIT extends MockAWSObjectsTest {
    @Autowired
    private PaperChannelSendClient client;

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
    void prepare890() {
        String requestId = "requestId";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/{requestId}"
                .replace("{requestId}", requestId);

        PaperChannelPrepareRequest paperChannelPrepareRequest = PaperChannelPrepareRequest.builder()
                .analogType(PhysicalAddressInt.ANALOG_TYPE.REGISTERED_LETTER_890)
                .requestId(requestId)
                .paAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .recipientInt(NotificationRecipientTestBuilder.builder().build())
                .notificationInt(NotificationTestBuilder.builder().build())
                .attachments(List.of("Att"))
                .build();
        
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );
        
        
        assertDoesNotThrow( ()  ->{
                    client.prepare(paperChannelPrepareRequest);
                }
        );
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void prepareAR() {
        String requestId = "requestId";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/{requestId}"
                .replace("{requestId}", requestId);

        PaperChannelPrepareRequest paperChannelPrepareRequest = PaperChannelPrepareRequest.builder()
                .analogType(PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER)
                .requestId(requestId)
                .paAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .recipientInt(NotificationRecipientTestBuilder.builder().build())
                .notificationInt(NotificationTestBuilder.builder().build())
                .attachments(List.of("Att"))
                .build();

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );


        assertDoesNotThrow( ()  ->{
                    client.prepare(paperChannelPrepareRequest);
                }
        );
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void prepareAR_secondrequest() {
        String requestId = "requestId";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/{requestId}"
                .replace("{requestId}", requestId);

        PaperChannelPrepareRequest paperChannelPrepareRequest = PaperChannelPrepareRequest.builder()
                .analogType(PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER)
                .requestId(requestId)
                .relatedRequestId("requestId_0")
                .recipientInt(NotificationRecipientTestBuilder.builder().build())
                .notificationInt(NotificationTestBuilder.builder().build())
                .attachments(List.of("Att"))
                .build();

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );


        assertDoesNotThrow( ()  ->{
                    client.prepare(paperChannelPrepareRequest);
                }
        );
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void prepareSimpleRegisteredLetter() {
        String requestId = "requestId";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/{requestId}"
                .replace("{requestId}", requestId);

        PaperChannelPrepareRequest paperChannelPrepareRequest = PaperChannelPrepareRequest.builder()
                .analogType(PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER)
                .requestId(requestId)
                .paAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .recipientInt(NotificationRecipientTestBuilder.builder().build())
                .notificationInt(NotificationTestBuilder.builder().build())
                .attachments(List.of("Att"))
                .build();

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );


        assertDoesNotThrow( ()  ->{
                    client.prepare(paperChannelPrepareRequest);
                }
        );
    }
    
    @Test
    void send() throws JsonProcessingException {

        String requestId = "requestId";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-send/{requestId}"
                .replace("{requestId}", requestId);

        SendResponse response = new SendResponse();
        int notificationCostExpected = 100;
        response.setAmount(notificationCostExpected);

        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(response);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(respJson)
                );
        
        PaperChannelSendRequest paperChannelSendRequest = PaperChannelSendRequest.builder()
                .requestId(requestId)
                .productType(ProductTypeEnum._890.getValue())
                .arAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .receiverAddress(PhysicalAddressInt.builder()
                        .address("test2")
                        .build())
                .recipientInt(NotificationRecipientTestBuilder.builder().build())
                .notificationInt(NotificationTestBuilder.builder().build())
                .attachments(List.of("Att"))
                .build();

        SendResponse sendResponse = client.send(paperChannelSendRequest);
        
        Assertions.assertEquals(notificationCostExpected, sendResponse.getAmount());

    }
}