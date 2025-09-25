package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkRequest.AttemptIdEnum;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkRequestedDetails implements ActionDetails {
    private AttemptIdEnum attemptId;
    private String reason;
    private String reworkId;
}