package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import reactor.core.publisher.Mono;

public interface PaperTrackerClient {

    Mono<SequenceResponse> retrieveSequenceAndFinalStatus(String statusCode, String deliveryFailureCause, String productType);
}
