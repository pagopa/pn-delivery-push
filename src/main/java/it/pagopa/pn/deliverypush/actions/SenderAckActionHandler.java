package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntryId;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.timeline.ReceivedDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
        addTimelineElement(action, TimelineElement.builder()
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(ReceivedDetails.builder()
                        .recipients(notification.getRecipients())
                        .documentsDigests(notification.getDocuments()
                                .stream()
                                .map(NotificationAttachment::getDigests)
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
