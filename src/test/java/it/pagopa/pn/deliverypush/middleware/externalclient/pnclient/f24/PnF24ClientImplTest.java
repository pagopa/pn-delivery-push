package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PnF24ClientImplTest {

    @Mock
    F24ControllerApi f24ControllerApi;

    PnF24ClientImpl client;

    @BeforeEach
    void setup() {
        client = new PnF24ClientImpl(f24ControllerApi);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void validate() {

        String cxId = "cxId";
        ValidateF24Request validateF24Request = new ValidateF24Request();
        validateF24Request.setSetId("setId");

        RequestAccepted response = new RequestAccepted();
        response.setDescription("description");
        response.setStatus("status");
        
        Mockito.when(f24ControllerApi.validateMetadata(cxId, validateF24Request.getSetId(), validateF24Request))
                .thenReturn(Mono.just(response));

        StepVerifier.create(client.validate(validateF24Request, cxId))
                .expectNext(response)
                .verifyComplete();
    }

}