package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED;
import static it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl.SAVE_LEGAL_FACT_EXCEPTION_MESSAGE;

@Component
@AllArgsConstructor
@Slf4j
public class ReceivedLegalFactCreationResponseHandler {
    private final NotificationService notificationService;
    private final AttachmentUtils attachmentUtils;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    
    public void handleReceivedLegalFactCreationResponse(String iun, String legalFactId) {
        log.info("Start handleReceivedLegalFactCreationResponse recipientWorkflow process - iun={} legalFactId={}", iun, legalFactId);
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        
        // cambio lo stato degli attachment in ATTACHED
        attachmentUtils.changeAttachmentsStatusToAttached(notification);
        
        addAcceptedRequestToTimeline(iun, legalFactId, notification);
        
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(notification);

        log.debug("End handleReceivedLegalFactCreationResponse - iun={}", notification.getIun());
    }

    private void addAcceptedRequestToTimeline(String iun, String legalFactId, NotificationInt notification) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "SaveNotificationReceivedLegalFact - iun={}", iun)
                .iun(iun)
                .build();
        logEvent.log();

        try {
            // aggiungo l'evento in timeline
            addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId), notification);

            logEvent.generateSuccess("SaveNotificationReceivedLegalFact success with fileKey={} - iun={}", legalFactId, notification.getIun()).log();
            
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED", notification.getIun(), "N/A");
            logEvent.generateFailure("Exception in saveNotificationReceivedLegalFact ex={}", exc).log();
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
