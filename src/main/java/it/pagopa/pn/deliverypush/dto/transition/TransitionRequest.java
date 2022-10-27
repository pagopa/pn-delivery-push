package it.pagopa.pn.deliverypush.dto.transition;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransitionRequest {

    private NotificationStatusInt fromStatus;
    private TimelineElementCategoryInt timelineRowType;
    private boolean multiRecipient;
}
