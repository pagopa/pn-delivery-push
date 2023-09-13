package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import reactor.core.publisher.Mono;

public interface NotificationCancellationService {
    Mono<StatusDetailInt>  startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType);
}
