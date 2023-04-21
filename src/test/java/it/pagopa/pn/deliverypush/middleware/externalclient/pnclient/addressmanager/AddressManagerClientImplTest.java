package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AnalogAddress;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsRequest;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeRequest;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.RecipientType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.address-manager-base-url=http://localhost:9998",
})
class AddressManagerClientImplTest {

    @Autowired
    private AddressManagerClientImpl client;

    private static ClientAndServer mockServer;

    @Test
    void normalizeAddresses() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/address-private/normalize";

        ObjectMapper mapper = new ObjectMapper();

        String internalId = "internalIdTest";

        BaseRecipientDto responseDto = new BaseRecipientDto();
        responseDto.setDenomination("denomination");
        responseDto.setInternalId(internalId);
        responseDto.setTaxId("taxId");
        responseDto.setRecipientType(RecipientType.PF);

        NormalizeItemsRequest request = new NormalizeItemsRequest();
        request.setCorrelationId("test");
        NormalizeRequest elem = new NormalizeRequest();
        elem.setAddress(new AnalogAddress());
        elem.setId("id");
        request.setRequestItems(List.of(elem));
        
        String requestJson = mapper.writeValueAsString(request);

        AcceptedResponse response = new AcceptedResponse();
        response.setCorrelationId("test");
        String responseJson = mapper.writeValueAsString(response);

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

        Mono<AcceptedResponse> responseMono = client.normalizeAddresses(request);
        
        Assertions.assertNotNull(responseMono);
        AcceptedResponse responseExpected = responseMono.block();
        Assertions.assertEquals(response, responseExpected);

        mockServer.stop();
    }
}