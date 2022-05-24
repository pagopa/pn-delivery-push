package it.pagopa.pn.deliverypush.actions;
/*
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class NotificationViewedActionHandler extends AbstractActionHandler {

    private final LegalFactDao legalFactStore;
    private final PaperNotificationFailedDao paperNotificationFailedDao;
    private final InstantNowSupplier instantSupplier;

    public NotificationViewedActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                           LegalFactDao legalFactStore, PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                           PaperNotificationFailedDao paperNotificationFailedDao,
                                           InstantNowSupplier instantSupplier) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedDao = paperNotificationFailedDao;
        this.instantSupplier = instantSupplier;
    }

    @Override
    public void handleAction(Action action, NotificationInt notification) {
    	NotificationRecipientInt recipient = notification.getRecipients().get( action.getRecipientIndex() );
        String legalFactKey = legalFactStore.saveNotificationViewedLegalFact( notification, recipient, instantSupplier.get() );
    	
        addTimelineElement(action, TimelineElementInternal.timelineInternalBuilder()
                .category( TimelineElementCategory.NOTIFICATION_VIEWED )
                .details( TimelineElementDetails.builder()
                        .taxId( recipient.getTaxId() )
                        .build()
                )
                .legalFactsIds( singleLegalFactId( legalFactKey, LegalFactCategory.RECIPIENT_ACCESS  ) )
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


