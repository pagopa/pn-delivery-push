package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;

@TestPropertySource(properties = {"pn.delivery-push.pagopa-cx-id=pn-delivery"})
class PnF24ClientImplTest {

    @Mock
    F24ControllerApi f24ControllerApi;

    @InjectMocks
    PnF24ClientImpl client;

    @Test
    @ExtendWith(SpringExtension.class)
    void validate(){

        RequestAccepted response = new RequestAccepted();
        response.setDescription("description");
        response.setStatus("status");

        Mockito.when(f24ControllerApi.validateMetadata(any(), any(), any())).thenReturn(Mono.just(response));

        StepVerifier.create(client.validate("setId"))
                .expectNext(response)
                .verifyComplete();
    }

}