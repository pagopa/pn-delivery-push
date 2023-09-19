package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCancellationServiceImpl implements NotificationCancellationService {
    private static final int FIRST_CANCELLATION_STEP = 1;
    private static final int SECOND_CANCELLATION_STEP = 2; //Da utilizzare per lo step async

    public static final String NOTIFICATION_ALREADY_CANCELLED = "NOTIFICATION_ALREADY_CANCELLED";
    public static final String NOTIFICATION_CANCELLATION_ACCEPTED = "NOTIFICATION_CANCELLATION_ACCEPTED";


    private final NotificationService notificationService;
    private final AuthUtils authUtils;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private AuditLogService auditLogService;
    
    @Override
    public Mono<StatusDetailInt> startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType) {

        PnAuditLogEvent logEvent = generateAuditLog(iun, FIRST_CANCELLATION_STEP);
        
        return notificationService.getNotificationByIunReactive(iun)
                .flatMap(notification -> 
                        authUtils.checkPaId(notification, paId, cxType)
                                .then(Mono.fromSupplier(() -> beginCancellationProcess(notification, logEvent)))
                )
                .doOnError(err -> logEvent.generateFailure("Error in cancellation process iun={} paId={}", iun, paId, err).log());
    }


    public void completeCancellationProcess(String iun){
        log.debug("Start cancelNotification - iun={}", iun);
        PnAuditLogEvent logEvent = generateAuditLog(iun, SECOND_CANCELLATION_STEP);

        try {
            // chiedo la cancellazione degli IUV
            notificationService.removeAllNotificationCostsByIun(iun).block();

            NotificationInt notification = notificationService.getNotificationByIun(iun);

            // salvo l'evento in timeline
            addCanceledTimelineElement(notification);

            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in cancellation process iun={}", iun, e).log();
            throw e;
        }

    }

    private void addCanceledTimelineElement(NotificationInt notification) {
        TimelineElementInternal cancelledTimelineElement = timelineUtils.buildCancelledTimelineElement(notification);
        // salvo l'evento in timeline
        timelineService.addTimelineElement(cancelledTimelineElement, notification);
    }

    private void addCancellationRequestTimelineElement(NotificationInt notification) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildCancelRequestTimelineElement(notification);
        timelineService.addTimelineElement(timelineElementInternal , notification);
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, int validationStep) {
        String message = "Notification cancellation step {} of 2.";
        
        return auditLogService.buildAuditLogEvent(
                iun,
                PnAuditLogEventType.AUD_NT_CANCELLED,
                message,
                validationStep
        );
    }

    private StatusDetailInt beginCancellationProcess(NotificationInt notification, PnAuditLogEvent logEvent)
    {
        if (!timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            addCancellationRequestTimelineElement(notification);
            logEvent.generateSuccess().log();
            return StatusDetailInt.builder()
                    .code(NOTIFICATION_CANCELLATION_ACCEPTED)
                    .level("INFO")
                    .detail("La richiesta di annullamento è stata presa in carico")
                    .build();
        }
        else
        {
            logEvent.generateWarning("Notification already cancelled iun={}", notification.getIun()).log();
            return StatusDetailInt.builder()
                    .code(NOTIFICATION_ALREADY_CANCELLED)
                    .level("WARN")
                    .detail("E' già presente una richiesta di annullamento per questa notifica")
                    .build();
        }
    }
}
