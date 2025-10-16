package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaperTrackerClientImpl implements PaperTrackerClient{

    private final NotificationReworkApi notificationReworkApi;

    @Override
    public Mono<SequenceResponse> retrieveSequenceAndFinalStatus(String statusCode, String deliveryFailureCause) {
        return notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, deliveryFailureCause)
                .onErrorResume(ex -> {
                    log.error("Error during retrieve sequence and finalStatus from paper-tracker for statusCode={} and deliveryFailureCause={}",
                            statusCode, deliveryFailureCause, ex);
                    return Mono.error(ex);
                });
    }
}
