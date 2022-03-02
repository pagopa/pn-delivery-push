package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationViewedDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class NotificationViewedActionHandler extends AbstractActionHandler {

    private final LegalFactUtils legalFactStore;
    private PaperNotificationFailedDao paperNotificationFailedDao;

    public NotificationViewedActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                           LegalFactUtils legalFactStore, PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                           PaperNotificationFailedDao paperNotificationFailedDao) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedDao = paperNotificationFailedDao;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
    	NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
        String legalFactKey = legalFactStore.saveNotificationViewedLegalFact( action, notification );
    	
        addTimelineElement(action, TimelineElement.builder()
                .category( TimelineElementCategory.NOTIFICATION_VIEWED )
                .details( NotificationViewedDetails.builder()
                        .taxId( recipient.getTaxId() )
                        .build()
                )
                .legalFactsIds( singleLegalFactId( legalFactKey, LegalFactType.RECIPIENT_ACCESS  ) )
                .build()
        );
        paperNotificationFailedDao.deleteNotificationFailed(recipient.getTaxId(),action.getIun() ); //Viene eliminata l'istanza di notifica fallita dal momento che la stessa è stata letta
    }

    @Override
    public ActionType getActionType() {
        return ActionType.NOTIFICATION_VIEWED;
    }
}
