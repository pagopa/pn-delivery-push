package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED;
import static it.pagopa.pn.deliverypush.service.impl.SaveLegalFactsServiceImpl.SAVE_LEGAL_FACT_EXCEPTION_MESSAGE;

@Component
@AllArgsConstructor
@Slf4j
public class ReceivedLegalFactCreationResponseHandler {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    
    public void handleReceivedLegalFactCreationResponse(String iun, String legalFactId) {
        
        PnAuditLogEvent logEvent = generateAuditLog(iun, legalFactId);
        logEvent.log();

        try {
            log.info("Start handleReceivedLegalFactCreationResponse recipientWorkflow process - iun={} legalFactId={}", iun, legalFactId);
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId), notification);

            scheduleRecipientWorkflow.startScheduleRecipientWorkflow(notification);

            logEvent.generateSuccess().log();

            log.debug("End handleReceivedLegalFactCreationResponse - iun={}", notification.getIun());
        }catch (Exception ex){
            logEvent.generateFailure("Saving legalFact FAILURE type={} fileKey={} iun={}", LegalFactCategoryInt.SENDER_ACK, legalFactId, iun, ex).log();
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, LegalFactCategoryInt.SENDER_ACK, iun, "N/A");
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, ex);
        }
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "Saving legalFact type={} fileKey={} iun={}", LegalFactCategoryInt.SENDER_ACK, legalFactId, iun)
                .iun(iun)
                .build();
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
