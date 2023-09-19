package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnF24ClientImpl extends CommonBaseClient implements PnF24Client {
    private final F24ControllerApi f24ControllerApi;

    @Override
    public Mono<RequestAccepted> validate(ValidateF24Request validateF24Request) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, VALIDATE_F24_PROCESS_NAME, "correlationId");
        return f24ControllerApi.validateMetadata(X_PAGOPA_CX_ID, validateF24Request.getSetId(), validateF24Request);
    }
}