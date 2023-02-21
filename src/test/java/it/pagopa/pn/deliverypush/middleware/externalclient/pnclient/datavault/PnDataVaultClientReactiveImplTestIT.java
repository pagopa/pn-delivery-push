package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.data-vault-base-url=http://localhost:9998",
//        "spring.cloud.stream.default.consumer.autoStartup=false"
})
class PnDataVaultClientReactiveImplTestIT extends MockAWSObjectsTest {
    @Autowired
    private PnDataVaultClientReactiveImpl client;
    
    private static ClientAndServer mockServer;
    
    @Test
    void getRecipientDenominationByInternalId() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/datavault-private/v1/recipients/internal";

        ObjectMapper mapper = new ObjectMapper();

        String internalId = "internalIdTest";

        BaseRecipientDto responseDto = new BaseRecipientDto();
        responseDto.setDenomination("denomination");
        responseDto.setInternalId(internalId);
        responseDto.setTaxId("taxId");
        responseDto.setRecipientType(RecipientType.PF);
        
        String responseJson = mapper.writeValueAsString(responseDto);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withBody(responseJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Flux<BaseRecipientDto> responseMono = client.getRecipientsDenominationByInternalId(List.of(internalId));
        Assertions.assertNotNull(responseMono);
        BaseRecipientDto response = responseMono.blockFirst();
        Assertions.assertEquals(responseDto, response);

        mockServer.stop();
    }

    @Test
    void getRecipientDenominationByInternalIdKo() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/datavault-private/v1/recipients/internal";
        String internalId = "internalId";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400)
                );

        Flux<BaseRecipientDto> responseMono = client.getRecipientsDenominationByInternalId(List.of(internalId));
        Assertions.assertNotNull(responseMono);
        
        Assertions.assertThrows( PnInternalException.class, responseMono::blockFirst);
        
        mockServer.stop();
    }
}