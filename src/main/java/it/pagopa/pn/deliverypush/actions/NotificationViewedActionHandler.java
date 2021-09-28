package it.pagopa.pn.deliverypush.actions;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationViewedDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class NotificationViewedActionHandler extends AbstractActionHandler {

    private final ActionsPool actionsPool;
    private final LegalFactUtils legalFactStore;

    public NotificationViewedActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                      LegalFactUtils legalFactStore, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.actionsPool = actionsPool;
        this.legalFactStore = legalFactStore;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
    	NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
    	
    	 addTimelineElement(action, TimelineElement.builder()
                 .category( TimelineElementCategory.NOTIFICATION_VIEWED )
                 .details( NotificationViewedDetails.builder()
                         .taxId( recipient.getTaxId() )
                         .build()
                 )
                 .build()
         );
    	 
    	 legalFactStore.saveNotificationViewedLegalFact( action, notification );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.NOTIFICATION_VIEWED;
    }
}
