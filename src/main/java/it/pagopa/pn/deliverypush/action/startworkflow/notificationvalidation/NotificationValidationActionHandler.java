package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.api.dto.events.PnF24MetadataValidationEndEventPayload;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.details.NotificationRefusedActionDetails;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.NormalizeAddressHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.config.SendMoreThan20GramsParameterConsumer;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.exceptions.*;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@CustomLog
public class NotificationValidationActionHandler {
    private static final int FIRST_VALIDATION_STEP = 1;
    private static final int SECOND_VALIDATION_STEP = 2;
    private static final int THIRD_VALIDATION_STEP = 3;
    private final AttachmentUtils attachmentUtils;
    private final TaxIdPivaValidator taxIdPivaValidator;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final NotificationValidationScheduler notificationValidationScheduler;
    private final AddressValidator addressValidator;
    private final AuditLogService auditLogService;
    private final NormalizeAddressHandler normalizeAddressHandler;
    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs cfg;
    private final F24Validator f24Validator;
    private final PaymentValidator paymentValidator;
    //quickWorkAroundForPN-9116
    private final SendMoreThan20GramsParameterConsumer parameterConsumer;
    private final SafeStorageService safeStorageService;
    private final DocumentComposition documentComposition;
    
    public void validateNotification(String iun, NotificationValidationActionDetails details){
        log.debug("Start validateNotification - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        PnAuditLogEvent logEvent = generateAuditLog(notification, FIRST_VALIDATION_STEP);
        
        try {
            paymentValidator.validatePayments(notification, details.getStartWorkflowTime());
            
            attachmentUtils.validateAttachment(notification);
            
            if(cfg.isCheckCfEnabled()){
                taxIdPivaValidator.validateTaxIdPiva(notification);
            }

            logEvent.generateSuccess().log();

            if (f24Exists(notification)) {
                //La validazione del F24 è async
                MDCUtils.addMDCToContextAndExecute(
                        f24Validator.requestValidateF24(notification)
                ).block();
            } else {
                String detail = " F24 does not exists, so F24 validation will be skipped";
                generateSkipAuditLog(notification, SECOND_VALIDATION_STEP, detail).generateSuccess().log();
                //La validazione dell'indirizzo è async
                MDCUtils.addMDCToContextAndExecute(
                        addressValidator.requestValidateAndNormalizeAddresses(notification)
                ).block();
            }

            //quickWorkAroundForPN-9116
            quickWorkAroundForPN9116(notification);

        } catch (PnValidationFileNotFoundException ex){
            if(cfg.isSafeStorageFileNotFoundRetry())
                logEvent.generateWarning("Validation need to be rescheduled - iun={} ex=", notification.getIun(), ex).log();
            handlePnValidationFileNotFoundException(iun, details, notification, ex, details.getStartWorkflowTime());
        } catch (PnValidationException ex){
            logEvent.generateWarning("Notification is not valid - iun={} ex=", notification.getIun(), ex).log();
            handleValidationError(notification, ex);
        } catch (RuntimeException ex){
            logEvent.generateWarning("Validation need to be rescheduled - iun={} ex=", notification.getIun(), ex).log();
            handleRuntimeException(iun, details, notification, ex, details.getStartWorkflowTime());
        }
    }

    private boolean f24Exists(NotificationInt notification) {
        return notification.getRecipients()
                .stream()
                .map(NotificationRecipientInt::getPayments)
                .anyMatch(notificationPaymentInfoIntV2s -> !CollectionUtils.isEmpty(notificationPaymentInfoIntV2s)
                        && notificationPaymentInfoIntV2s
                        .stream()
                        .anyMatch(paymentInfoIntV2 -> paymentInfoIntV2.getF24() != null));
    }

    private void handleRuntimeException(String iun, NotificationValidationActionDetails details, NotificationInt notification, RuntimeException ex, Instant startWorkflowTime) {
        log.warn(String.format("RuntimeException in validateNotification - iun=%s", iun), ex);
        log.info("Notification validation need to be rescheduled for ex={} - iun={}", ex, iun);
        notificationValidationScheduler.scheduleNotificationValidation(notification, details.getRetryAttempt(), ex, startWorkflowTime);
    }

    private void handlePnValidationFileNotFoundException(String iun, NotificationValidationActionDetails details, NotificationInt notification, PnValidationFileNotFoundException ex, Instant startWorkflowTime) {
    /* Per la PnValidationFileNotFoundException la notifica non viene portata in rifiutata MA è prevista una gestione ad hoc. Questo avviene 
       perchè al momento non c'è possibilità di distinguere un 404 dovuto ad un mancato caricamento file da parte della PA (che dovrebbe portare
       regolarmente la notifica in rifiutata) e un 404 dovuto ad un ritardo nel caricamento del file nel bucket corretto da parte di
       safeStorage (in questo caso si di deve procedere con i ritentativi). Si sceglie dunque per ore di ritentare in entrambi i casi
    */
        log.warn(String.format("File not found exception in validateNotification - iun=%s", iun), ex);
        if(cfg.isSafeStorageFileNotFoundRetry()) {
            log.info("Notification validation need to be rescheduled  - iun={}", iun);
            notificationValidationScheduler.scheduleNotificationValidation(notification, details.getRetryAttempt(), ex, startWorkflowTime);
        } else {
            handleValidationError(notification, ex);
        }
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(NotificationInt notification, int validationStep) {
        String message = "Notification validation step {} of 3.";

        if(! cfg.isCheckCfEnabled()){
            message += " TaxId validation will be skipped";
        }

        return auditLogService.buildAuditLogEvent(
                notification.getIun(),
                PnAuditLogEventType.AUD_NT_VALID,
                message,
                validationStep
        );
    }

    @NotNull
    private PnAuditLogEvent generateSkipAuditLog(NotificationInt notification, int validationStep, String detail) {
        String message = "Notification validation step {} of 3." + detail;

        return auditLogService.buildAuditLogEvent(
                notification.getIun(),
                PnAuditLogEventType.AUD_NT_VALID,
                message,
                validationStep
        );
    }

    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<NotificationRefusedErrorInt> errors = new ArrayList<>();
        if (Objects.nonNull( ex.getProblem() )) {
            ex.getProblem().getErrors().forEach( elem -> {
                NotificationRefusedErrorInt notificationRefusedError = NotificationRefusedErrorInt.builder()
                        .errorCode(elem.getCode())
                        .detail(elem.getDetail())
                        .build();
                
                errors.add(notificationRefusedError);
            });
        }

        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        scheduleNotificationRefused(notification.getIun(), errors);
    }

    public void scheduleNotificationRefused(String iun, List<NotificationRefusedErrorInt> errors) {
        Instant schedulingDate = Instant.now();

        NotificationRefusedActionDetails details = NotificationRefusedActionDetails.builder()
                .errors(errors)
                .build();

        log.debug("Scheduling Notification refused schedulingDate={} - iun={}", schedulingDate, iun);
        schedulerService.scheduleEvent(iun, schedulingDate, ActionType.NOTIFICATION_REFUSED, details);
    }

    public void handleValidateF24Response(PnF24MetadataValidationEndEventPayload metadataValidationEndEvent) {
        NotificationInt notification = notificationService.getNotificationByIun(metadataValidationEndEvent.getSetId());
        PnAuditLogEvent logEvent = generateAuditLog(notification, SECOND_VALIDATION_STEP);
        try {
            if (!CollectionUtils.isEmpty(metadataValidationEndEvent.getErrors())) {
                List<String> errors = metadataValidationEndEvent.getErrors().stream()
                        .map(error -> "ERROR: " + error.getCode() + " \n" +
                                "ON ELEMENT: " +  error.getElement() + " \n" +
                                "MESSAGE: " +  error.getDetail())
                        .toList();
                throw new PnValidationNotValidF24Exception(errors);
            } else {
                timelineService.addTimelineElement(
                        timelineUtils.buildValidatedF24TimelineElement(notification),
                        notification
                );

                MDCUtils.addMDCToContextAndExecute(
                        addressValidator.requestValidateAndNormalizeAddresses(notification)
                ).block();

                logEvent.generateSuccess().log();
            }
        } catch (PnValidationException e) {
            logEvent.generateWarning("Notification is not valid - iun={} ex={}", notification.getIun(), e).log();
            handleValidationError(notification, e);
        }
    }

    public void handleValidateAndNormalizeAddressResponse(String iun, NormalizeItemsResultInt normalizeItemsResult) {

        NotificationInt notification = notificationService.getNotificationByIun(iun);
        PnAuditLogEvent logEvent = generateAuditLog(notification, THIRD_VALIDATION_STEP);

        try {
            addressValidator.handleAddressValidation(iun, normalizeItemsResult);
            normalizeAddressHandler.handleNormalizedAddressResponse(notification, normalizeItemsResult);
            
            log.debug("Notification validated successfully - iun={}", iun);
            
            Instant schedulingDate = Instant.now();
            log.debug("Scheduling received legalFact generation, schedulingDate={} - iun={}", schedulingDate, iun);
            schedulerService.scheduleEvent(iun, schedulingDate, ActionType.SCHEDULE_RECEIVED_LEGALFACT_GENERATION);

            logEvent.generateSuccess().log();
        } catch (PnValidationNotValidAddressException ex){
            logEvent.generateWarning("Notification is not valid - iun={} ex={}", notification.getIun(), ex).log();
            handleValidationError(notification, ex);
        }

    }

    /**
     *
     * quickWorkAroundForPN-9116
     */
    private void quickWorkAroundForPN9116(NotificationInt notification) {
        if(!canSendMoreThan20Grams(notification.getSender().getPaTaxId())) {
            final String errorDetail = String.format( "Validation failed, sender paTaxId=%s can't send more than 20 grams.", notification.getSender().getPaTaxId());
            if(haveSomePayments(notification)) {
                throw new PnValidationMoreThan20GramsException(errorDetail + " Some attachment payments");
            }
            int numberOfDocuments = notification.getDocuments().size();
            switch (numberOfDocuments) {
                case 1: checkDocumentsMaxPageNumber(notification, 4);
                case 2: checkDocumentsMaxPageNumber(notification, 2);
                default: throw new PnValidationMoreThan20GramsException(errorDetail + " More than two documents");
            }
        }
    }

    private void checkDocumentsMaxPageNumber(NotificationInt notification, int maxPages) {
        for (NotificationDocumentInt doc : notification.getDocuments() ) {
            NotificationDocumentInt.Ref ref = doc.getRef();
            FileDownloadResponseInt fd = MDCUtils.addMDCToContextAndExecute(
                            safeStorageService.getFile(ref.getKey(), false)
                                    .onErrorResume(PnFileNotFoundException.class, this::handleNotFoundError))
                    .block();
            byte[] pieceOfContent = safeStorageService.downloadPieceOfContent(fd.getKey(), fd.getDownload().getUrl(), -1).block();
            int actualPages = documentComposition.getNumberOfPageFromPdfBytes(pieceOfContent);
            if (actualPages > maxPages) {
                final String errorDetail = String.format( "Validation failed, sender paTaxId=%s can't send document=%s with page=%s. Max pages=%s",
                        notification.getSender().getPaTaxId(),
                        fd.getKey(),
                        actualPages,
                        maxPages
                );
                throw new PnValidationMoreThan20GramsException(errorDetail);
            }
        }
    }

    @NotNull
    private Mono<FileDownloadResponseInt> handleNotFoundError(PnFileNotFoundException ex) {
        //log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, false, ex.getMessage());
        return Mono.error(
                new PnValidationFileNotFoundException(
                        ex.getMessage(),
                        ex.getCause()
                )
        );
    }

    private boolean haveSomePayments(NotificationInt notification) {
        return notification.getRecipients().stream().anyMatch( recipientInt -> !CollectionUtils.isEmpty(recipientInt.getPayments()));
    }
    private boolean canSendMoreThan20Grams(String paTaxId) {
        return parameterConsumer.isPaEnabledToSendMoreThan20Grams(paTaxId);
    }
}
