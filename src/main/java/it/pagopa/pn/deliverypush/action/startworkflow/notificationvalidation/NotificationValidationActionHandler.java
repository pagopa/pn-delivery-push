package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationValidationActionHandler {

    private final AttachmentUtils attachmentUtils;
    private final TaxIdPivaValidator taxIdPivaValidator;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    private final NotificationValidationScheduler notificationValidationScheduler;
    private final AuditLogService auditLogService;
    private final PnDeliveryPushConfigs cfg;

    public void validateNotification(String iun, NotificationValidationActionDetails details){
        log.info("Start validateNotification - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        PnAuditLogEvent logEvent = generateAuditLog(notification);

        try {
            attachmentUtils.validateAttachment(notification);
            taxIdPivaValidator.validateTaxIdPiva(notification);
            
            log.info("Notification validated successfully - iun={}", iun);
            receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(notification);

            logEvent.generateSuccess().log();
        } catch (PnValidationFileNotFoundException ex){
            if(cfg.isSafeStorageFileNotFoundRetry())
                logEvent.generateWarning("Validation need to be rescheduled - iun={} ex={}", notification.getIun(), ex).log();
            handlePnValidationFileNotFoundException(iun, details, notification, ex);
        } catch (PnValidationException ex){
            logEvent.generateWarning("Notification is not valid - iun={} ex={}", notification.getIun(), ex).log();
            handleValidationError(notification, ex);
        } catch (RuntimeException ex){
            logEvent.generateWarning("Validation need to be rescheduled - iun={} ex={}", notification.getIun(), ex).log();
            handleRuntimeException(iun, details, notification, ex);
        }
    }

    private void handleRuntimeException(String iun, NotificationValidationActionDetails details, NotificationInt notification, RuntimeException ex) {
        log.warn(String.format("RuntimeException in validateNotification - iun=%s", iun), ex);
        log.info("Notification validation need to be rescheduled for ex={} - iun={}", ex, iun);
        notificationValidationScheduler.scheduleNotificationValidation(notification, details.getRetryAttempt(), ex);
    }

    private void handlePnValidationFileNotFoundException(String iun, NotificationValidationActionDetails details, NotificationInt notification, PnValidationFileNotFoundException ex) {
    /* Per la PnValidationFileNotFoundException la notifica non viene portata in rifiutata MA è prevista una gestione ad hoc. Questo avviene 
       perchè al momento non c'è possibilità di distinguere un 404 dovuto ad un mancato caricamento file da parte della PA (che dovrebbe portare
       regolarmente la notifica in rifiutata) e un 404 dovuto ad un ritardo nel caricamento del file nel bucket corretto da parte di
       safeStorage (in questo caso si di deve procedere con i ritentativi). Si sceglie dunque per ore di ritentare in entrambi i casi
    */
        log.warn(String.format("File not found exception in validateNotification - iun=%s", iun), ex);
        ex.getProblem().setErrors(List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.toString())
                .detail("Allegati non trovati")
                .build()));
        if(cfg.isSafeStorageFileNotFoundRetry()) {
            log.info("Notification validation need to be rescheduled  - iun={}", iun);
            notificationValidationScheduler.scheduleNotificationValidation(notification, details.getRetryAttempt(), ex);
        } else {
            handleValidationError(notification, ex);
        }
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(NotificationInt notification) {
        return auditLogService.buildAuditLogEvent(notification.getIun(), PnAuditLogEventType.AUD_NT_VALID, "Notification validation iun={}", notification.getIun());
    }

    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<NotificationRefusedErrorInt> errors = new ArrayList<>();
        if (Objects.nonNull( ex.getProblem() )) {
            ex.getProblem().getErrors().forEach( elem -> {
                //Per sviluppi futuri si può pensare d'inserire questo intero oggetto in timeline
                NotificationRefusedErrorInt notificationRefusedError = NotificationRefusedErrorInt.builder()
                        .errorCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.valueOf(elem.getCode()))
                        .detail(elem.getDetail())
                        .build();
                
                errors.add(notificationRefusedError);
            });
        }
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
