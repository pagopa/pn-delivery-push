package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.PaperTrackerStatusResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;

public class PaperTrackerClientMock implements PaperTrackerClient {

    @Override
    public PaperTrackerStatusResponse getPaperStatus(String prepareAnalogDomicileTimelineId) {
        PaperTrackerStatusResponse paperTrackerStatusResponse = new PaperTrackerStatusResponse();
        paperTrackerStatusResponse.setFinalDemaStatusFound(true);
        return paperTrackerStatusResponse;
    }
}
