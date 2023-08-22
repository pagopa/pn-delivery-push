package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class FailureWorkflowHandler {
    private final RefinementScheduler refinementScheduler;
    private final RegisteredLetterSender registeredLetterSender;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;

    public void scheduleRefinementAndSendRegisteredLetter(NotificationInt notification, int recIndex, Instant completionWorkflowDate){
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, completionWorkflowDate, EndWorkflowStatus.FAILURE);
        registeredLetterSender.prepareSimpleRegisteredLetter(notification, recIndex);
    }
    
    public void handleFailureWorkflow(NotificationInt notification, int recIndex, PnAuditLogEvent logEvent, DigitalDeliveryCreationRequestDetailsInt timelineDetails) {
        String legalFactId = timelineDetails.getLegalFactId();
                
        addFailureWorkflowTimelineElement(recIndex, notification, logEvent, legalFactId);
    }

    private void addFailureWorkflowTimelineElement(int recIndex, NotificationInt notification, PnAuditLogEvent logEvent, String legalFactId) {
        try {
            timelineService.addTimelineElement(timelineUtils.buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId), notification);
            logEvent.generateSuccess().log();
        }catch (Exception ex){
            logEvent.generateFailure("Error in buildFailureDigitalWorkflowTimelineElement legalFactId={} - iun={} recIndex={}", legalFactId, notification.getIun(), recIndex, ex).log();
            throw ex;
        }
    }
}
