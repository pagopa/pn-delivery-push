package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.GenerateF24Request;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@CustomLog
@RequiredArgsConstructor
@Component
public class PnF24ClientImpl implements PnF24Client {
    private final F24ControllerApi f24ControllerApi;


    @Override
    public Mono<Void> validate(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, "validate");

        ResponseEntity<SendMessageResponse> resp;
        return f24ControllerApi.validate(iun);
    }

    @Override
    public Mono<Void> generateAllPDF(String requestId, GenerateF24Request generateF24Request) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, "generateAllPDF", requestId);

        return f24ControllerApi.generateAllPDF(requestId, generateF24Request);
    }

}
