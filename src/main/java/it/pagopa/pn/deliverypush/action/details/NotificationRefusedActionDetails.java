package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRefusedActionDetails implements ActionDetails {
    private List<NotificationRefusedErrorInt> errors;
}
