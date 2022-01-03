package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.RefinementDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

@Component
public class WaitForRecipientTimeoutActionHandler extends AbstractActionHandler {

    public WaitForRecipientTimeoutActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
    }

    @Override
    public void handleAction(Action action, Notification notification) {

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        addTimelineElement(action, TimelineElement.builder()
                .category(TimelineElementCategory.REFINEMENT)
                .details(RefinementDetails.builder()
                        .taxId(recipient.getTaxId())
                        .build()
                )
                .build()
        );

    }

    @Override
    public ActionType getActionType() {
        return ActionType.WAIT_FOR_RECIPIENT_TIMEOUT;
    }
}
