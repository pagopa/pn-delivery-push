package it.pagopa.pn.deliverypush.actions;
/*
import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import ReceivedDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class SenderAckActionHandler extends AbstractActionHandler {

    private final LegalFactUtils legalFactStore;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public SenderAckActionHandler(LegalFactUtils legalFactStore, TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.legalFactStore = legalFactStore;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public void handleAction(Action action, Notification notification) {

        String legalFactKey = legalFactStore.saveNotificationReceivedLegalFact(action, notification);

        // - GENERATE NEXT ACTIONS
        int numberOfRecipients = notification.getRecipients().size();
        for (int idx = 0; idx < numberOfRecipients; idx++) {
            Action nextAction = Action.builder()
                    .iun(action.getIun())
                    .type(ActionType.CHOOSE_DELIVERY_MODE)
                    .notBefore(Instant.now().plus(pnDeliveryPushConfigs.getTimeParams().getWaitingForNextAction()))
                    .recipientIndex(idx)
                    .build();
            scheduleAction(nextAction);
        }

        // - WRITE TIMELINE
        addTimelineElement(action, TimelineElementInternal.timelineInternalBuilder()
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(ReceivedDetails.builder()
                        .recipients(notification.getRecipients())
                        .documentsDigests(notification.getDocuments()
                                .stream()
                                .map(NotificationDocumentInt::getDigests)
                                .collect(Collectors.toList())
                        )
                        .build()
                )
                .legalFactsIds( singleLegalFactId( legalFactKey, LegalFactType.SENDER_ACK ) )
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SENDER_ACK;
    }
}

 */
