package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkValidationDetails implements ActionDetails {
    private String reworkId;
    private String reworkAttempt;
    private String reworkPcRetry;
    private String reworkRecIndex;
    private String reworkExpectedFinalStatus;
    private String reason;
}
