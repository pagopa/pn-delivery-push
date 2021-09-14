package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import org.springframework.stereotype.Component;

@Component
public class WaitForRecipientTimeoutActionHandler extends AbstractActionHandler {

    public WaitForRecipientTimeoutActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super( timelineDao, actionsPool, pnDeliveryPushConfigs);
    }

    @Override
    public void handleAction(Action action, Notification notification ) {

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        addTimelineElement(action, TimelineElement.builder()
                .category( TimelineElementCategory.WAIT_FOR_RECIPIENT_TIMEOUT )
                .details( WaitForRecipientTimeoutDetails.builder()
                        .taxId( recipient.getTaxId() )
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
