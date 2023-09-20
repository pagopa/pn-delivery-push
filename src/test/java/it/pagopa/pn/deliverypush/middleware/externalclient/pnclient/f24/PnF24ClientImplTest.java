package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class PnF24ClientImplTest {

    @Mock
    F24ControllerApi f24ControllerApi;

    @InjectMocks
    PnF24ClientImpl client;

    @Test
    @ExtendWith(SpringExtension.class)
    void validate(){

        ValidateF24Request validateF24Request = new ValidateF24Request();
        validateF24Request.setSetId("setId");

        RequestAccepted response = new RequestAccepted();
        response.setDescription("description");
        response.setStatus("status");

        Mockito.when(f24ControllerApi.validateMetadata("pn-delivery", validateF24Request.getSetId(), validateF24Request)).thenReturn(Mono.just(response));

        Mono<RequestAccepted> responseMono = client.validate(validateF24Request);

        Assertions.assertNotNull(responseMono);
        RequestAccepted responseExpected = responseMono.block();
        Assertions.assertEquals(response, responseExpected);
    }

}