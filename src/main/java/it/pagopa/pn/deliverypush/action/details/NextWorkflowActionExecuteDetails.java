package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NextWorkflowActionExecuteDetails implements ActionDetails {
  private DigitalAddressInfoSentAttempt lastAttemptMade;
  private DigitalAddressInfoSentAttempt nextAddressInfo;
}
