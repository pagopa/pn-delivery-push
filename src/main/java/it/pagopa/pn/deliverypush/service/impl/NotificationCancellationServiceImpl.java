package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
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

    private final NotificationService notificationService;
    private final AuthUtils authUtils;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private AuditLogService auditLogService;
    
    @Override
    public Mono<Void> startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType) {

        PnAuditLogEvent logEvent = generateAuditLog(iun, FIRST_CANCELLATION_STEP);
        
        return notificationService.getNotificationByIunReactive(iun)
                .flatMap(notification -> 
                        authUtils.checkPaId(notification, paId, cxType)
                                .then(Mono.fromRunnable(() ->{
                                    addCancellationRequestTimelineElement(notification);
                                    logEvent.generateSuccess().log();
                                }))
                )
                .then()
                .doOnError(err -> logEvent.generateFailure("Error in cancellation process iun={} paId={}", iun, paId, err).log());
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
}
