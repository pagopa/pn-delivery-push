package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.PaperStatusApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.PaperTrackerStatusResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@RequiredArgsConstructor
public class PaperTrackerClientImpl implements PaperTrackerClient {
    private final PaperStatusApi paperStatusApi;

    public PaperTrackerStatusResponse getPaperStatus(String prepareAnalogDomicileTimelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_PAPER_STATUS);
        return paperStatusApi.paperStatus(prepareAnalogDomicileTimelineId, null);
    }
}
