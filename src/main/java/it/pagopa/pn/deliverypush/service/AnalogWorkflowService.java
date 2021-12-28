package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;

public interface AnalogWorkflowService {
    int getSentAttemptFromTimeLine(String iun, String taxId);

    SendPaperFeedbackDetails getLastTimelineSentFeedback(String iun, String taxId);

}
