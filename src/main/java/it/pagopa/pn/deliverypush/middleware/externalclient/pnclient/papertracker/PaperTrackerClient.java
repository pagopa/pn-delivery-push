package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.PaperTrackerStatusResponse;

public interface PaperTrackerClient {
    String CLIENT_NAME = "pn-paper-tracker"; //TODO replace with common's constant
    String GET_PAPER_STATUS = "GET PAPER STATUS";

    PaperTrackerStatusResponse getPaperStatus(String prepareAnalogDomicileTimelineId);
}
