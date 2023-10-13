package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationValidationActionDetails implements ActionDetails {
  private int retryAttempt;
  private Instant startWorkflowTime;
}
