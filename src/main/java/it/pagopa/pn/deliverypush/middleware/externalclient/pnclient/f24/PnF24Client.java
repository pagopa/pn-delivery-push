package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.ValidateF24Request;
import reactor.core.publisher.Mono;

public interface PnF24Client {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_F24;
    String VALIDATE_F24_PROCESS_NAME = "VALIDATE F24";
    String X_PAGOPA_CX_ID = "pn-delivery";

    Mono<RequestAccepted> validate(ValidateF24Request validateF24Request);
}
 