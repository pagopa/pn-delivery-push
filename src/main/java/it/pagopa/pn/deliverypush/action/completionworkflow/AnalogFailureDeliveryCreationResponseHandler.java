package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogFailureWorkflowCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class AnalogFailureDeliveryCreationResponseHandler {
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final RefinementScheduler refinementScheduler;

    public void handleAnalogFailureDeliveryCreationResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleAnalogFailureDeliveryCreationResponse - iun={} recIndex={} aarKey={}", iun, recIndex, actionDetails.getKey());
        NotificationInt notification = notificationService.getNotificationByIun(iun);





        PnAuditLogEvent logEvent = createAuditLog(notification, recIndex, actionDetails.getKey());
        logEvent.log();
        
        Optional<AnalogFailureWorkflowCreationRequestDetailsInt> analogFailureWorkflowCreationRequestDetailsIntOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), AnalogFailureWorkflowCreationRequestDetailsInt.class);

        if (analogFailureWorkflowCreationRequestDetailsIntOpt.isPresent()) {
            AnalogFailureWorkflowCreationRequestDetailsInt timelineDetails = analogFailureWorkflowCreationRequestDetailsIntOpt.get();


            timelineService.addTimelineElement(timelineUtils.buildFailureAnalogWorkflowTimelineElement(notification, recIndex, timelineDetails.getLegalFactId()), notification);
            completelyUnreachableUtils.handleCompletelyUnreachable(notification, recIndex);
            refinementScheduler.scheduleAnalogRefinement(notification, recIndex, timelineDetails.getCompletionWorkflowDate(), timelineDetails.getEndWorkflowStatus());

        } else {
            logEvent.generateFailure("Error in handleAnalogFailureDeliveryCreationResponse for timelineId={} and key={} - iun={} recIndex={}", actionDetails.getTimelineId(), actionDetails.getKey(), notification.getIun(), recIndex).log();
        }
    }

    private PnAuditLogEvent createAuditLog(NotificationInt notification, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "Saving legalFact type={} fileKey={} - iun={} recIndex={}", LegalFactCategoryInt.ANALOG_FAILURE_DELIVERY, legalFactId, notification.getIun(), recIndex)
                .iun(notification.getIun())
                .build();
    }
}
