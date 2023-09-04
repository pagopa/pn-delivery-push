package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import reactor.core.publisher.Mono;

public interface NotificationCancellationService {
    Mono<Void>  startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType);
}
