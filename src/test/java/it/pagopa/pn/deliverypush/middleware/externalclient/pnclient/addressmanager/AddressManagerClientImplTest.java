package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.config.msclient.AddressManagerApiReactiveConfigurator;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AnalogAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.RecipientType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.address-manager-base-url=http://localhost:9998",
        "pn.delivery-push.address-manager-api-key=testApiKey"
})

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AddressManagerClientImpl.class,
        PnDeliveryPushConfigs.class,
        AddressManagerApiReactiveConfigurator.class
})
class AddressManagerClientImplTest extends MockAWSObjectsTest {

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