package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.RequestAccepted;
import reactor.core.publisher.Mono;

public interface PnF24Client {

    //TODO: DECOMMENTARE QUANDO SARA' DISPONIBILE COMMONS
    //String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_F24;
    String CLIENT_NAME = "pn-f24";
    String VALIDATE_F24_PROCESS_NAME = "VALIDATE F24";

    Mono<RequestAccepted> validate(String iun);
}
 