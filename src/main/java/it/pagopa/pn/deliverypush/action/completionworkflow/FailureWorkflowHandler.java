package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class FailureWorkflowHandler {
    private final MVPParameterConsumer mvpParameterConsumer;
    private final RefinementScheduler refinementScheduler;
    private final RegisteredLetterSender registeredLetterSender;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;

    public void handleFailureWorkflow(NotificationInt notification, int recIndex, PnAuditLogEvent logEvent, DigitalDeliveryCreationRequestDetailsInt timelineDetails) {
        String senderTaxId = notification.getSender().getPaTaxId();
        Instant legalFactCreationDate = Instant.now();
        EndWorkflowStatus status = timelineDetails.getEndWorkflowStatus();
        String legalFactId = timelineDetails.getLegalFactId();
                
        addFailureWorkflowTimelineElement(recIndex, notification, logEvent, legalFactId, legalFactCreationDate);

        if (Boolean.FALSE.equals(mvpParameterConsumer.isMvp(senderTaxId))) {
            refinementScheduler.scheduleDigitalRefinement(notification, recIndex, legalFactCreationDate, status);
            registeredLetterSender.prepareSimpleRegisteredLetter(notification, recIndex);
        } else {
            boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);
            if (!isNotificationAlreadyViewed) {
                log.info("Paper message is not handled, registered Letter will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                addPaperNotificationNotHandledToTimeline(notification, recIndex);
            } else {
                log.info("Notification is already viewed, it will not go into the cancelled state - iun={} recipientIndex={}", notification.getIun(), recIndex);
            }
        }
    }

    private void addFailureWorkflowTimelineElement(int recIndex, NotificationInt notification, PnAuditLogEvent logEvent, String legalFactId, Instant legalFactCreationDate) {
        try {
            timelineService.addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId, legalFactCreationDate), notification);
            logEvent.generateSuccess().log();
        }catch (Exception ex){
            logEvent.generateFailure("Error in buildFailureDigitalWorkflowTimelineElement legalFactId={} - iun={} recIndex={} exc=", legalFactId, notification.getIun(), recIndex, ex).log();
            throw ex;
        }
    }

    private void addPaperNotificationNotHandledToTimeline(NotificationInt notification, Integer recIndex) {
        timelineService.addTimelineElement(
                timelineUtils.buildNotHandledTimelineElement(
                        notification,
                        recIndex,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_CODE,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_REASON
                ),
                notification
        );
    }
}
