package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class NotificationReworkUpdateValidationDetails {
        private String reworkId;
        private String reworkExpectedDeliveryFailureCause;
        private List<SequenceItemInternal> reworkExpectedStatusCodes;
        private String reworkExpectedFinalStatus;
        private String reworkAttempt;
        private String reworkRecIndex;
}