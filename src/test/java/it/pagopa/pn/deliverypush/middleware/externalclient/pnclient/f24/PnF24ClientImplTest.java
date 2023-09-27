package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@TestPropertySource(properties = {"pn.delivery-push.pagopa-cx-id=pn-delivery"})
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

        Mono<RequestAccepted> responseMono = client.validate("setId");

        Assertions.assertNotNull(responseMono);
        RequestAccepted responseExpected = responseMono.block();
        Assertions.assertEquals(response, responseExpected);
    }

}