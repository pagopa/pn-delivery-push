package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.api.dto.notification.timeline.CompletelyUnreachableDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.middleware.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CompletelyUnreachableActionHandler extends AbstractActionHandler {
    private PaperNotificationFailedDao paperNotificationFailedDao;

    protected CompletelyUnreachableActionHandler(TimelineDao timelineDao, PaperNotificationFailedDao paperNotificationFailedDao,
                                                 ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.paperNotificationFailedDao = paperNotificationFailedDao;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
        log.info("Start CompletelyUnreachableActionHandler:handleAction");
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        // - GENERATE NEXT ACTIONS
        Action nextAction = buildWaitRecipientTimeoutActionForUnreachable(action);
        scheduleAction(nextAction);

        if (!isNotificationAlreadyViewed(action)) {
            addPaperNotificationFailed(notification, recipient);
        }
        buildAndAddTimeLineElement(action, recipient);
        log.info("End CompletelyUnreachableActionHandler:handleAction");
    }

    private boolean isNotificationAlreadyViewed(Action action) {
        //Lo user potrebbe aver visualizzato la notifica tramite canali differenti anche se non raggiunto dai canali 'legali'
        return this.isPresentTimeLineElement(action, ActionType.NOTIFICATION_VIEWED);
    }

    private void addPaperNotificationFailed(Notification notification, NotificationRecipient recipient) {
        paperNotificationFailedDao.addPaperNotificationFailed(
                PaperNotificationFailed.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .build()
        );
    }

    private void buildAndAddTimeLineElement(Action action, NotificationRecipient recipient) {
        addTimelineElement(action, TimelineElement.builder()
                .category(TimelineElementCategory.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetails.builder()
                        .taxId(recipient.getTaxId())
                        .build()
                )
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.COMPLETELY_UNREACHABLE;
    }
}
