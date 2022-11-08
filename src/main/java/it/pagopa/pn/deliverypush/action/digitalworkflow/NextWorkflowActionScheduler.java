package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

import java.time.Instant;

public interface NextWorkflowActionScheduler {
    void scheduleNextWorkflowAction(NotificationInt notification,
                                           Integer recIndex,
                                           DigitalAddressInfoSentAttempt lastAttemptMade,
                                           Instant schedulingDate);

    void scheduleNextWorkflowAction7Days(NotificationInt notification,
                                                Integer recIndex,
                                                DigitalAddressInfoSentAttempt lastAttemptMade,
                                                Instant schedulingDate);
}
