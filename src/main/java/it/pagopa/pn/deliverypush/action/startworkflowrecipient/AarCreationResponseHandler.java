package it.pagopa.pn.deliverypush.action.startworkflowrecipient;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class AarCreationResponseHandler {
    private AarUtils aarUtils;
    private NotificationService notificationService;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final SchedulerService schedulerService;
    private final TimelineService timelineService;
    
    public void handleAarCreationResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleAarCreationResponse recipientWorkflow process - iun={} aarKey={}", iun, actionDetails.getKey());

        PnAuditLogEvent logEvent = generateAuditLog(iun, recIndex, actionDetails.getKey());
        logEvent.log();
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        
        try {
            storingAarResponse(iun, recIndex, actionDetails, logEvent, notification);
        } catch (Exception ex){
            logEvent.generateFailure("Saving AAR FAILURE fileKey={} iun={} recIndex={} ex={}", actionDetails.getKey(), iun, recIndex, ex).log();
            throw new PnInternalException("Saving AAR exception", ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, ex);
        }
        
        //... Invio messaggio di cortesia ... 
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, recIndex);

        //... e viene schedulato il processo di scelta della tipologia di notificazione
        scheduleChooseDeliveryMode(iun, recIndex);
    }

    private void storingAarResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails, PnAuditLogEvent logEvent, NotificationInt notification) {
        Optional<AarCreationRequestDetailsInt> aarCreationDetailsOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), AarCreationRequestDetailsInt.class);

        if(aarCreationDetailsOpt.isPresent()){
            AarCreationRequestDetailsInt timelineDetails = aarCreationDetailsOpt.get();

            PdfInfo pdfInfo = PdfInfo.builder()
                    .key(actionDetails.getKey())
                    .numberOfPages(timelineDetails.getNumberOfPages())
                    .build();

            aarUtils.addAarGenerationToTimeline(notification, recIndex, pdfInfo);
            logEvent.generateSuccess().log();
        } else {
            log.error("handleAarCreationResponse failed, timelineId is not present {} - iun={} id={}", actionDetails.getTimelineId(), iun, recIndex);
            throw new PnInternalException("AarCreationRequestDetails timelineId is not present", ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }

    private void scheduleChooseDeliveryMode(String iun, Integer recIndex) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling choose delivery mode schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.CHOOSE_DELIVERY_MODE);
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_AAR, "Saving AAR fileKey={} iun={} recIndex={}", legalFactId, iun, recIndex)
                .iun(iun)
                .build();
    }

}
