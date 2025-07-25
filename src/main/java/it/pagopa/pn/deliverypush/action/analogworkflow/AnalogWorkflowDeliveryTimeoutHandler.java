package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogTimeoutCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
@CustomLog
public class AnalogWorkflowDeliveryTimeoutHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;

    public void handleDeliveryTimeout(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleDeliveryTimeout process - iun={} recIndex={} legalFactId={}", iun, recIndex, actionDetails.getKey());
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        PnAuditLogEvent auditLogEvent = generateAuditLog(iun, recIndex, actionDetails.getKey());
        try {
            Optional<SendAnalogTimeoutCreationRequestDetailsInt> sendAnalogTimeoutCreationRequestDetailsOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), SendAnalogTimeoutCreationRequestDetailsInt.class);
            if (sendAnalogTimeoutCreationRequestDetailsOpt.isPresent()) {
                SendAnalogTimeoutCreationRequestDetailsInt timelineDetails = sendAnalogTimeoutCreationRequestDetailsOpt.get();
                Integer sentAttemptMade = timelineDetails.getSentAttemptMade();
                Instant timeoutDate = timelineDetails.getTimeoutDate();
                switch (sentAttemptMade) {
                    case 0:
                        handleFirstAttempt(actionDetails, notification, recIndex, timeoutDate, sentAttemptMade, auditLogEvent);
                        break;
                    case 1:
                        handleSecondAttempt(actionDetails, notification, recIndex, timeoutDate, auditLogEvent);
                }
            }
        } catch (Exception ex) {
            throw new PnInternalException("Unexpected error: " + ex.getMessage(), PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }

    private void handleFirstAttempt(DocumentCreationResponseActionDetails actionDetails,
                                    NotificationInt notification,
                                    int recIndex,
                                    Instant timeoutDate,
                                    Integer sentAttemptMade,
                                    PnAuditLogEvent auditLogEvent){
        String iun = notification.getIun();
        log.info("First sent attempt - iun={} id={}", iun, recIndex);
        buildSendAnalogTimeoutElement(actionDetails, notification, recIndex, timeoutDate, auditLogEvent);
        boolean isNotificationViewed = timelineUtils.checkIsNotificationViewed(iun, recIndex);

        if (isNotificationViewed) {
            log.info("Notification with iun={} viewed by recipient with index={}, second attempt will not be scheduled", iun, recIndex);
        } else {
            log.info("Notification with iun={} not viewed by recipient with index={}, second attempt will be scheduled", iun, recIndex);
            int sentAttemptMadeForSecondAttempt = sentAttemptMade + 1;
            analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMadeForSecondAttempt, null);
        }
    }

    private void buildSendAnalogTimeoutElement(DocumentCreationResponseActionDetails actionDetails,
                                               NotificationInt notification,
                                               int recIndex,
                                               Instant timeoutDate,
                                               PnAuditLogEvent auditLogEvent){
        String iun = notification.getIun();
        Optional<SendAnalogDetailsInt> sendAnalogDetailsOpt =
                timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), SendAnalogDetailsInt.class);
        if (sendAnalogDetailsOpt.isPresent()) {
            SendAnalogDetailsInt sendAnalogDetails = sendAnalogDetailsOpt.get();
            TimelineElementInternal sendAnalogTimeoutElementInternal = timelineUtils.buildSendAnalogTimeout(notification, sendAnalogDetails, timeoutDate, actionDetails.getKey());
            timelineService.addTimelineElement(sendAnalogTimeoutElementInternal, notification);
            auditLogEvent.generateSuccess("SEND_ANALOG_TIMEOUT successfully added for recIndex={}", recIndex).log();
        } else {
            log.error("SendAnalogDetails not found for iun={} and timelineId={}", iun, actionDetails.getTimelineId());
        }
    }

    private void handleSecondAttempt(DocumentCreationResponseActionDetails actionDetails,
                                     NotificationInt notification,
                                     int recIndex,
                                     Instant timeoutDate,
                                     PnAuditLogEvent auditLogEvent){
        log.info("Second sent attempt - iun={} id={}", notification.getIun(), recIndex);
        try {
            buildSendAnalogTimeoutElement(actionDetails, notification, recIndex, timeoutDate, auditLogEvent);
            analogDeliveryTimeoutUtils.buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);
            auditLogEvent.generateSuccess("ANALOG_FAILURE_WORKFLOW_TIMEOUT successfully added for recIndex={}", recIndex).log();
        } catch (Exception exc) {
            auditLogEvent.generateFailure("Unexpected error", exc).log();
            throw exc;
        }
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_ANALOG_TIMEOUT, "Saving legalFact type={} fileKey={} - iun={} recIndex={}", LegalFactCategoryInt.ANALOG_DELIVERY_TIMEOUT, legalFactId, iun, recIndex)
                .iun(iun)
                .build();
    }
}
