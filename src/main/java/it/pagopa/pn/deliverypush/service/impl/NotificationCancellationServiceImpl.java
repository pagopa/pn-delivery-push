package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.PaymentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.action.utils.PaymentUtils.handleResponse;
import static it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCancellationServiceImpl implements NotificationCancellationService {
    public static final int NOTIFICATION_CANCELLED_COST = 0;

    private static final int FIRST_CANCELLATION_STEP = 1;
    private static final int SECOND_CANCELLATION_STEP = 2; //Da utilizzare per lo step async
    private static final int THIRD_CANCELLATION_STEP = 3;

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
    private final SaveLegalFactsService saveLegalFactsService;
    private final DocumentCreationRequestService documentCreationRequestService;

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

    public void continueCancellationProcess(String iun){
        log.debug("Start continueCancellationProcess - iun={}", iun);
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

            // genero e faccio upload del documento di annullamento
            String legalFactId = saveLegalFactsService.sendCreationRequestForNotificationCancelledLegalFact(notification, getNotificationCancellationRequestDate(iun));

            // salvo l'evento in timeline
            TimelineElementInternal timelineElementInternal = addNotificationCancelledLegalFactTimelineElement(notification, legalFactId);

            // vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
            documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.NOTIFICATION_CANCELLED, timelineElementInternal.getElementId());

            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in continueCancellationProcess iun={}", iun, e).log();
            throw e;
        }
    }

    @Override
    public void completeCancellationProcess(String iun, String legalFactId) {
        log.debug("Start completeCancellationProcess - iun={}, legalFactId={}", iun, legalFactId);
        PnAuditLogEvent logEvent = generateAuditLog(iun, THIRD_CANCELLATION_STEP);

        try {
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            // salvo l'evento in timeline
            addCanceledTimelineElement(notification, legalFactId);

            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in completeCancellationProcess iun={}, legalFactId={}", iun, legalFactId, e).log();
            throw e;
        }
    }

    private Instant getNotificationCancellationRequestDate(String iun) {
        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        return timelineService.getTimelineElement(iun, elementId)
                .orElseThrow(() -> new IllegalStateException("Timeline element not found"))
                .getTimestamp();
    }

    private void handleUpdateNotificationCost(NotificationInt notification) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = PaymentUtils.getPaymentsInfoFromNotification(notification);
        Instant timestampSendUpdate = Instant.now();

        if( !paymentsInfoForRecipients.isEmpty() ){
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
        }else {
            log.debug("Don't need to update notification cost, paymentsInfoForRecipients is empty - iun={}", notification.getIun());
        }

    }

    private void addCanceledTimelineElement(NotificationInt notification, String legalFactId) {
        TimelineElementInternal cancelledTimelineElement = timelineUtils.buildCancelledTimelineElement(notification, legalFactId);
        // salvo l'evento in timeline
        timelineService.addTimelineElement(cancelledTimelineElement, notification);
    }

    private void addCancellationRequestTimelineElement(NotificationInt notification) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildCancelRequestTimelineElement(notification);
        timelineService.addTimelineElement(timelineElementInternal , notification);
    }

    private TimelineElementInternal addNotificationCancelledLegalFactTimelineElement(NotificationInt notification, String legalFactId) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationCancelledLegalFactCreationRequest(notification, legalFactId);
        timelineService.addTimelineElement(timelineElementInternal, notification);
        return timelineElementInternal;
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, int validationStep) {
        String message = "Notification cancellation step {} of 3.";
        
        return auditLogService.buildAuditLogEvent(
                iun,
                PnAuditLogEventType.AUD_NT_CANCELLED,
                message,
                validationStep
        );
    }

    private StatusDetailInt beginCancellationProcess(NotificationInt notification, PnAuditLogEvent logEvent)
    {
        if(!timelineUtils.checkIsNotificationRefused(notification.getIun())) {
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
