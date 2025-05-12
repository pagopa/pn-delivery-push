package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final ActionsPool actionsPool;
    private final Clock clock;
    private final TimelineUtils timelineUtils;

    @Override
    public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType) {
        this.scheduleEvent(iun, null, dateToSchedule, actionType, null, null);
    }
    
    @Override
    public void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails){
        this.scheduleEvent(iun, null, dateToSchedule, actionType, null, actionDetails);
    }

    @Override
    public void scheduleEventNowOnlyIfAbsent(String iun, ActionType actionType, ActionDetails actionDetails){
        this.scheduleEvent(iun, null, Instant.now(), actionType, null, actionDetails);
    }

    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null, actionDetails);
    }
    
    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType) {
        this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, null, null);
    }

    @Override
    public void scheduleEvent(
            String iun,
            Integer recIndex,
            Instant dateToSchedule,
            ActionType actionType,
            String timelineEventId,
            ActionDetails actionDetails
    ) {
        log.info("Schedule {} in schedulingDate={} - iun={}", actionType, dateToSchedule, iun);

        if (!timelineUtils.checkIsNotificationCancellationRequested(iun) || checkIsDocumentForNotificationCancelled(actionDetails)) {
            Action action = Action.builder()
                    .iun(iun)
                    .recipientIndex(recIndex)
                    .notBefore(dateToSchedule)
                    .type(actionType)
                    .timelineId(timelineEventId)
                    .details(actionDetails)
                    .build();

            action = action.toBuilder()
                    .actionId(action.getType().buildActionId(action))
                    .build();

            log.debug("ScheduleEvent iun={} recIndex={} dateToSchedule={} actionType={} timelineEventId={} actionId={}", iun, recIndex, dateToSchedule, actionType, timelineEventId, action.getActionId());
            actionsPool.addOnlyAction(action);
        } else {
            log.info("Notification is cancelled, the action {} will not be scheduled - iun={}", actionType, iun);
        }
    }

    private boolean checkIsDocumentForNotificationCancelled(ActionDetails actionDetails) {
        DocumentCreationResponseActionDetails documentCreationDetails = getDocumentCreationResponseActionDetails(actionDetails);
        return documentCreationDetails != null && documentCreationDetails.getDocumentCreationType() == DocumentCreationTypeInt.NOTIFICATION_CANCELLED;
    }

    private DocumentCreationResponseActionDetails getDocumentCreationResponseActionDetails(ActionDetails actionDetails) {
        DocumentCreationResponseActionDetails documentCreationDetails = null;

        if(actionDetails instanceof DocumentCreationResponseActionDetails)
            documentCreationDetails = (DocumentCreationResponseActionDetails) actionDetails;
        return documentCreationDetails;
    }

    @Override
    public void unscheduleEvent(String iun, Integer recIndex, ActionType actionType, String timelineEventId) {
        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .type(actionType)
                .timelineId(timelineEventId)
                .build();

        this.actionsPool.unscheduleFutureAction (action.getType().buildActionId(action));
    }

    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule,
        ActionType actionType, String timelineId) {
      this.scheduleEvent(iun, recIndex, dateToSchedule, actionType, timelineId, null);
    }
    
}
