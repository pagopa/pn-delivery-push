package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.GenerateF24Request;
import reactor.core.publisher.Mono;

public interface PnF24Client {
    String CLIENT_NAME = "pn-f24";
    
    Mono<Void> validate(String iun);

    Mono<Void> generateAllPDF(String requestId, GenerateF24Request generateF24Request);
}
