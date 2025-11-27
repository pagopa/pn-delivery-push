package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
@CustomLog
public class PaperTrackerClientImpl implements PaperTrackerClient{

    private final NotificationReworkApi notificationReworkApi;

    private static final String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_PAPER_TRACKER;
    private static final String OPERATION_RETRIEVE_SEQUENCE_AND_FINAL_STATUS = "RETRIEVE_SEQUENCE_AND_FINAL_STATUS";

    @Override
    public Mono<SequenceResponse> retrieveSequenceAndFinalStatus(String statusCode, String deliveryFailureCause, String productType) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, OPERATION_RETRIEVE_SEQUENCE_AND_FINAL_STATUS, String.format("%s - %s - %s", statusCode, deliveryFailureCause, productType));
        return notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, productType, deliveryFailureCause);
    }
}
