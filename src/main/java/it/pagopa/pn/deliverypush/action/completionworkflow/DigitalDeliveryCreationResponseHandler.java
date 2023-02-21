package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
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
public class  DigitalDeliveryCreationResponseHandler {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final SuccessWorkflowHandler successWorkflowHandler;
    private final FailureWorkflowHandler failureWorkflowHandler;

    public void handleDigitalDeliveryCreationResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleDigitalDeliveryCreationResponse - iun={} recIndex={} aarKey={}", iun, recIndex, actionDetails.getKey());
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        PnAuditLogEvent logEvent = createAuditLog(notification, recIndex, actionDetails.getKey());
        logEvent.log();
        
        Optional<DigitalDeliveryCreationRequestDetailsInt> digitalDeliveryLegalFactCreationDetailsOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), DigitalDeliveryCreationRequestDetailsInt.class);

        if (digitalDeliveryLegalFactCreationDetailsOpt.isPresent()) {
            DigitalDeliveryCreationRequestDetailsInt timelineDetails = digitalDeliveryLegalFactCreationDetailsOpt.get();
            EndWorkflowStatus status = timelineDetails.getEndWorkflowStatus();
            
            if (status != null) {
                switch (status) {
                    case SUCCESS ->
                            successWorkflowHandler.handleSuccessWorkflow(notification, recIndex, logEvent, timelineDetails);
                    case FAILURE ->
                            failureWorkflowHandler.handleFailureWorkflow(notification, recIndex, logEvent, timelineDetails);
                    default -> handleError(iun, recIndex, status, logEvent, actionDetails.getKey());
                }
            } else 
                handleError(iun, recIndex, null, logEvent, actionDetails.getKey());

        } else {
            logEvent.generateFailure("Error in handleDigitalDeliveryCreationResponse for timelineId={} and key={} - iun={} recIndex={}", actionDetails.getTimelineId(), actionDetails.getKey(), notification.getIun(), recIndex).log();
        }
    }

    private void handleError(String iun, Integer recIndex, EndWorkflowStatus status, PnAuditLogEvent logEvent, String key) {
        logEvent.generateFailure("Error in handleDigitalDeliveryCreationResponse, not valid status={} and key={} - iun={} recIndex={}", status, key, iun, recIndex).log();
        log.error("Specified status {} does not exist. Iun {}, id {}", status, iun, recIndex);
        throw new PnInternalException("Specified status " + status + " does not exist. Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
    }

    private PnAuditLogEvent createAuditLog(NotificationInt notification, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "Saving legalFact type={} fileKey={} - iun={} recIndex={}", LegalFactCategoryInt.DIGITAL_DELIVERY, legalFactId, notification.getIun(), recIndex)
                .iun(notification.getIun())
                .build();
    }
}
