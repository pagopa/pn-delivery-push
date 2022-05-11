package it.pagopa.pn.deliverypush.actions;
/*
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import RefinementDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

@Component
public class WaitForRecipientTimeoutActionHandler extends AbstractActionHandler {

    public WaitForRecipientTimeoutActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
    }

    @Override
    public void handleAction(Action action, Notification notification) {

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        addTimelineElement(action, TimelineElementInternal.timelineInternalBuilder()
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

 */
