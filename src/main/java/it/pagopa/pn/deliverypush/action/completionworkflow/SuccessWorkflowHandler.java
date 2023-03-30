package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
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
public class SuccessWorkflowHandler {
    private final RefinementScheduler refinementScheduler;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public void handleSuccessWorkflow(NotificationInt notification, int recIndex,  PnAuditLogEvent logEvent, DigitalDeliveryCreationRequestDetailsInt timelineDetails) {
        
        EndWorkflowStatus status = timelineDetails.getEndWorkflowStatus();
        Instant completionWorkflowDate = timelineDetails.getCompletionWorkflowDate();
        String legalFactId = timelineDetails.getLegalFactId();

        addSuccessWorkflowTimelineElement(recIndex, notification, logEvent, timelineDetails.getDigitalAddress(), legalFactId);
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, completionWorkflowDate, status);
    }

    private void addSuccessWorkflowTimelineElement(int recIndex, NotificationInt notification, PnAuditLogEvent logEvent, LegalDigitalAddressInt digitalAddress, String legalFactId) {
        try {
            timelineService.addTimelineElement(timelineUtils.buildSuccessDigitalWorkflowTimelineElement(notification, recIndex, digitalAddress, legalFactId), notification);
            logEvent.generateSuccess().log();
        }catch (Exception ex){
            logEvent.generateFailure("Error in buildSuccessDigitalWorkflowTimelineElement legalFactId={} - iun={} recIndex={} exc=", legalFactId, notification.getIun(), recIndex, ex).log();
            throw ex;
        }
    }
}
