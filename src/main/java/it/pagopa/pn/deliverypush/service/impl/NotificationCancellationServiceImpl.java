package it.pagopa.pn.deliverypush.service.impl;

import static it.pagopa.pn.deliverypush.action.utils.PaymentUtils.handleResponse;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.PaymentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCancellationServiceImpl implements NotificationCancellationService {
    public static final int NOTIFICATION_CANCELLED_COST = 0;

    private static final int FIRST_CANCELLATION_STEP = 1;
    private static final int SECOND_CANCELLATION_STEP = 2; //Da utilizzare per lo step async

    public static final String NOTIFICATION_ALREADY_CANCELLED = "NOTIFICATION_ALREADY_CANCELLED";
    public static final String NOTIFICATION_CANCELLATION_ACCEPTED = "NOTIFICATION_CANCELLATION_ACCEPTED";
    public static final String NOTIFICATION_REFUSED = "NOTIFICATION_REFUSED";


    private final NotificationService notificationService;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final AuthUtils authUtils;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private AuditLogService auditLogService;
    private NotificationProcessCostService notificationProcessCostService;

    @Override
    public Mono<StatusDetailInt> startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType, List<String> xPagopaPnCxGroups) {

        PnAuditLogEvent logEvent = generateAuditLog(iun, FIRST_CANCELLATION_STEP);
        
        return notificationService.getNotificationByIunReactive(iun)
                .flatMap(notification -> 
                        authUtils.checkPaIdAndGroup(notification, paId, cxType, xPagopaPnCxGroups)
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

            if(NotificationFeePolicy.DELIVERY_MODE.equals(notification.getNotificationFeePolicy()) &&
                    PagoPaIntMode.ASYNC.equals(notification.getPagoPaIntMode())){
                handleUpdateNotificationCost(notification);
            } else {
                log.debug("don't need to update notification cost - iun={}", iun);
            }

            // elimino le righe di paper notification failed
            notification.getRecipients().forEach(recipient ->
                    paperNotificationFailedService.deleteNotificationFailed(recipient.getInternalId(), iun));

            // salvo l'evento in timeline
            addCanceledTimelineElement(notification);

            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in cancellation process iun={}", iun, e).log();
            throw e;
        }
    }

    private void handleUpdateNotificationCost(NotificationInt notification) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = PaymentUtils.getPaymentsInfoFromNotification(notification);
        Instant timestampSendUpdate = Instant.now();

        UpdateNotificationCostResponseInt updateNotificationCostResponse = notificationProcessCostService.setNotificationStepCost(
                NOTIFICATION_CANCELLED_COST,
                notification.getIun(),
                paymentsInfoForRecipients,
                timestampSendUpdate,
                timestampSendUpdate,
                UpdateCostPhaseInt.NOTIFICATION_CANCELLED
        ).block();

        if (updateNotificationCostResponse != null && !updateNotificationCostResponse.getUpdateResults().isEmpty()) {
            handleResponse(notification, updateNotificationCostResponse);
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
        if(timelineUtils.checkIsNotificationAccepted(notification.getIun())) {
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
        } else {
            logEvent.generateWarning("Notification refused iun={}", notification.getIun()).log();
            return StatusDetailInt.builder()
                    .code(NOTIFICATION_REFUSED)
                    .level("WARN")
                    .detail("Notifica rifiutata")
                    .build();
        }

    }
}
