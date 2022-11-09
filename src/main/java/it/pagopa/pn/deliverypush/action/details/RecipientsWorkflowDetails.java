package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecipientsWorkflowDetails implements ActionDetails {

  private String quickAccessLinkToken;
}
