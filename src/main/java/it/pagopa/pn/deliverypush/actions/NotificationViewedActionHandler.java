package it.pagopa.pn.deliverypush.actions;
/*
import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.deliverypush.middleware.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import NotificationViewedDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

import java.time.Instant;

@Component
public class NotificationViewedActionHandler extends AbstractActionHandler {

    private final LegalFactUtils legalFactStore;
    private final PaperNotificationFailedDao paperNotificationFailedDao;

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
        String legalFactKey = legalFactStore.saveNotificationViewedLegalFact( notification, recipient, Instant.now() );
    	
        addTimelineElement(action, TimelineElementInternal.timelineInternalBuilder()
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

 */
