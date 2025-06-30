package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;

import java.time.Instant;

public interface AnalogWorkflowTimeoutHandlerService {

    void handleAnalogWorkflowTimeout(String iun, String timelineId, Integer recIndex,
                                     AnalogWorkflowTimeoutDetails details, Instant notBefore);
}
