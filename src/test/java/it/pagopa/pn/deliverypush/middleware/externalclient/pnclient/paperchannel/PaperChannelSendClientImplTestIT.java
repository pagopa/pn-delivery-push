package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.exceptions.PnPaperChannelChangedCostException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.Problem;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.ProblemError;
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
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
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
                .aarWithRadd(true)
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
                .aarWithRadd(false)
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
                .aarWithRadd(null)
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
    void prepareSimpleRegisteredLetter() {
        String requestId = "requestId";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-prepare/{requestId}"
                .replace("{requestId}", requestId);

        // si vuole verificare la data sentAt sia presente nel body della request
        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withSentAt(Instant.EPOCH.plusMillis(57))
                .withIun("iun_12345")
                .build();

        PaperChannelPrepareRequest paperChannelPrepareRequest = PaperChannelPrepareRequest.builder()
                .analogType(PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER)
                .requestId(requestId)
                .paAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .recipientInt(NotificationRecipientTestBuilder.builder()
                        .withTaxId("GeneratedTaxId_9ce24c59-862c-4024-aa75-40d888e6acac").build())
                .notificationInt(notificationInt)
                .attachments(List.of("Att"))
                .aarWithRadd(true)
                .build();

        // notare che il campo sentAt nel body non viene serializzato nel formato data
        String body = """
                {"proposalProductType":"RS","notificationSentAt":0.057000000,"iun":"iun_12345","requestId":"requestId","receiverFiscalCode":"GeneratedTaxId_9ce24c59-862c-4024-aa75-40d888e6acac","receiverType":"PF","receiverAddress":{"fullname":null,"address":"test","city":null},"printType":"BN_FRONTE_RETRO","attachmentUrls":["Att"]}""";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                        .withBody(body)
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



    @Test
    void send_Unprocessable() throws JsonProcessingException {

        String requestId = "requestId1";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-send/{requestId}"
                .replace("{requestId}", requestId);

        Problem response = new Problem();
        response.setStatus(422);
        response.setDetail("costo");
        response.setErrors(List.of(new ProblemError()));

        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(response);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(422)
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

        Assertions.assertThrows(PnPaperChannelChangedCostException.class, () -> client.send(paperChannelSendRequest));



    }


    @Test
    void send_ERror() throws JsonProcessingException {

        String requestId = "requestId2";
        String path = "/paper-channel-private/v1/b2b/paper-deliveries-send/{requestId}"
                .replace("{requestId}", requestId);

        Problem response = new Problem();
        response.setStatus(422);
        response.setDetail("costo");
        response.setErrors(List.of(new ProblemError()));

        ObjectMapper mapper = new ObjectMapper();
        String respJson = mapper.writeValueAsString(response);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("POST")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(500)
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

        Assertions.assertThrows(PnHttpResponseException.class, () -> client.send(paperChannelSendRequest));



    }
}