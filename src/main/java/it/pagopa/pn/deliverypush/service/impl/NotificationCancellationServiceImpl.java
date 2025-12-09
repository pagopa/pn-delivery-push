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

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCancellationServiceImpl implements NotificationCancellationService {
    private static final int FIRST_CANCELLATION_STEP = 1;

    public static final String NOTIFICATION_ALREADY_CANCELLED = "NOTIFICATION_ALREADY_CANCELLED";
    public static final String NOTIFICATION_CANCELLATION_ACCEPTED = "NOTIFICATION_CANCELLATION_ACCEPTED";
    public static final String NOTIFICATION_REFUSED = "NOTIFICATION_REFUSED";


    private final NotificationService notificationService;
    private final AuthUtils authUtils;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private AuditLogService auditLogService;

    @Override
    public Mono<StatusDetailInt> startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType, List<String> xPagopaPnCxGroups) {

        PnAuditLogEvent logEvent = generateAuditLog(iun);
        
        return notificationService.getNotificationByIunReactive(iun)
                .flatMap(notification -> 
                        authUtils.checkPaIdAndGroup(notification, paId, cxType, xPagopaPnCxGroups)
                                .then(Mono.fromSupplier(() -> beginCancellationProcess(notification, logEvent)))
                )
                .doOnError(err -> logEvent.generateFailure("Error in cancellation process iun={} paId={}", iun, paId, err).log());
    }
    private void addCancellationRequestTimelineElement(NotificationInt notification) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildCancelRequestTimelineElement(notification);
        timelineService.addTimelineElement(timelineElementInternal , notification);
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun) {
        String message = "Notification cancellation step {} of 3.";
        
        return auditLogService.buildAuditLogEvent(
                iun,
                PnAuditLogEventType.AUD_NT_CANCELLED,
                message,
                NotificationCancellationServiceImpl.FIRST_CANCELLATION_STEP
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
