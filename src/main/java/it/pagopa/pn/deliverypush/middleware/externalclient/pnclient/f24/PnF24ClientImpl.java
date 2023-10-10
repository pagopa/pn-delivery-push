package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.api.F24ControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@CustomLog
public class PnF24ClientImpl extends CommonBaseClient implements PnF24Client {
    private final F24ControllerApi f24ControllerApi;
    private final PnDeliveryPushConfigs cfg;

    public PnF24ClientImpl(F24ControllerApi f24ControllerApi, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.f24ControllerApi = f24ControllerApi;
        this.cfg = pnDeliveryPushConfigs;
    }

    @Override
    public Mono<RequestAccepted> validate(String iun) {
        ValidateF24Request validateF24Request = new ValidateF24Request().setId(iun);
        log.logInvokingAsyncExternalService(CLIENT_NAME, VALIDATE_F24_PROCESS_NAME, iun);
        return f24ControllerApi.validateMetadata(cfg.getF24CxId(), iun, validateF24Request);
    }
}