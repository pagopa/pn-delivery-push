package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.PaymentsInfoForRecipient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResult;
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

import java.util.Collections;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.external-registry-base-url=http://localhost:9998",
})
class PnExternalRegistriesClientReactiveImplTest extends MockAWSObjectsTest {
    @Autowired
    private PnExternalRegistriesClientReactive client;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9998);
    }
    
    @Test
    void updateNotificationCost() throws JsonProcessingException {
        //Given
        String path = "/ext-registry-private/cost-update";

        ObjectMapper mapper = new ObjectMapper();

        UpdateNotificationCostRequest request = new UpdateNotificationCostRequest();
        final String iun = "iun";
        request.setIun(iun);
        request.setNotificationStepCost(100);
        final PaymentsInfoForRecipient paymentsInfoForRecipient = new PaymentsInfoForRecipient()
                .creditorTaxId("testcred")
                .noticeCode("testCod")
                .recIndex(0);
        request.setPaymentsInfoForRecipients(Collections.singletonList(paymentsInfoForRecipient));
        request.setUpdateCostPhase(UpdateNotificationCostRequest.UpdateCostPhaseEnum.NOTIFICATION_CANCELLED);
        
        String requestJson = mapper.writeValueAsString(request);

        UpdateNotificationCostResponse response = new UpdateNotificationCostResponse();
        response.setIun(iun);
        response.setUpdateResults(Collections.singletonList(
                new UpdateNotificationCostResult()
                        .creditorTaxId(paymentsInfoForRecipient.getCreditorTaxId())
                        .noticeCode(paymentsInfoForRecipient.getNoticeCode())
                        .recIndex(paymentsInfoForRecipient.getRecIndex())
        ));
        
        String responseJson = mapper.writeValueAsString(response);

        try (MockServerClient mockServerClient = new MockServerClient("localhost", 9998)) {
            mockServerClient
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

            Mono<UpdateNotificationCostResponse> responseMono = client.updateNotificationCost(request);

            Assertions.assertNotNull(responseMono);
            UpdateNotificationCostResponse responseExpected = responseMono.block();
            Assertions.assertEquals(response, responseExpected);
        }
    }
    
}