package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestNotificationViewedDto;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class NotificationViewedRequestHandler {

    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final StatusUtils statusUtils;
    private final NotificationUtils notificationUtils;
    private final ViewNotification viewNotification;
    private final PaperNotificationFailedService paperNotificationFailedService;


    public NotificationViewedRequestHandler(TimelineService timelineService,
                                            NotificationService notificationService,
                                            TimelineUtils timelineUtils,
                                            StatusUtils statusUtils,
                                            NotificationUtils notificationUtils,
                                            ViewNotification viewNotification,
                                            PaperNotificationFailedService paperNotificationFailedService) {
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
        this.statusUtils = statusUtils;
        this.notificationUtils = notificationUtils;
        this.viewNotification = viewNotification;
        this.paperNotificationFailedService = paperNotificationFailedService;
    }
    
    //La richiesta proviene da delivery (La visualizzazione potrebbe essere da parte del delegato o da parte del destinatario)
    public void handleViewNotificationDelivery(String iun, Integer recIndex, DelegateInfoInt delegateInfo, Instant eventTimestamp) {
        MDCUtils.addMDCToContextAndExecute(
                handleViewNotification(iun, recIndex, null, delegateInfo, eventTimestamp)
        ).block();
    }

    //La richiesta proviene da RADD, visualizzazione da parte del destinatario 
    public Mono<Void> handleViewNotificationRadd(String iun, Integer recIndex, RaddInfo raddInfo, Instant eventTimestamp) {
        return handleViewNotification(iun, recIndex, raddInfo, null, eventTimestamp);
    }
    
    private Mono<Void> handleViewNotification(String iun, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo, Instant eventTimestamp) {
        
        return Mono.fromCallable(() -> (
                timelineUtils.checkIsNotificationCancellationRequested(iun)))
            .flatMap( isNotificationCancelled -> {
                if (Boolean.TRUE.equals(isNotificationCancelled)){
                    log.warn("For this notification a cancellation has been requested - iun={} id={}", iun, recIndex);
                    return Mono.empty();
                } else {
                    return Mono.just(timelineUtils.checkIsNotificationViewed(iun, recIndex));
                }
            })
            .flatMap( isNotificationAlreadyViewed -> {
                
                //I processi collegati alla visualizzazione di una notifica vengono effettuati solo la prima volta che la stessa viene visualizzata
                if(Boolean.FALSE.equals(isNotificationAlreadyViewed) ){

                    PnAuditLogEvent logEvent = generateAuditLog(iun, recIndex, raddInfo, delegateInfo);
                    logEvent.log();

                    log.debug("Notification is not already viewed - iun={} id={}", iun, recIndex);

                    return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                            .flatMap( notification -> {
                                            NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                                            return viewNotification.startVewNotificationProcess(notification, recipient, recIndex, raddInfo, delegateInfo, eventTimestamp)
                                                .doOnSuccess( x->
                                                    logEvent.generateSuccess().log()
                                                );
                                    })
                            .doOnError( err -> logEvent.generateFailure("Exception in View notification iun={} id={}", iun, recIndex, err).log());
                } else {
                    log.debug("Notification is already viewed - iun={} id={}", iun, recIndex);
                    return Mono.empty();
                }
            });
    }

    private PnAuditLogEvent generateAuditLog(String iun, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo ) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        boolean viewedFromDelegate = delegateInfo != null;
        
        PnAuditLogEventType type = viewedFromDelegate ? PnAuditLogEventType.AUD_NT_VIEW_DEL : PnAuditLogEventType.AUD_NT_VIEW_RCP;
        return auditLogBuilder
                .before(type, "View notification - iun={} id={} " +
                                "raddType={} raddTransactionId={} internalDelegateId={} mandateId={}",
                        iun,
                        recIndex,
                        raddInfo != null ? raddInfo.getType() : null,
                        raddInfo != null ? raddInfo.getTransactionId() : null,
                        viewedFromDelegate ? delegateInfo.getInternalId() : null,
                        viewedFromDelegate ? delegateInfo.getMandateId() : null
                )
                .iun(iun)
                .build();
    }

    public Mono<Void> handleNotificationRaddRetrieved(String iun, RequestNotificationViewedDto requestNotificationViewedDto) {
        return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                .flatMap( notification -> {
                    int recIndex = notificationUtils.getRecipientIndexFromInternalId(notification, requestNotificationViewedDto.getRecipientInternalId());

                    RaddInfo raddInfo = RaddInfo.builder()
                            .type(requestNotificationViewedDto.getRaddType())
                            .transactionId(requestNotificationViewedDto.getRaddBusinessTransactionId())
                            .build();
                    return handleViewNotificationRaddRetrieved(iun, recIndex, requestNotificationViewedDto.getRecipientInternalId(), raddInfo, requestNotificationViewedDto.getRaddBusinessTransactionDate());
                });
    }

    private Mono<Void> handleViewNotificationRaddRetrieved(String iun, Integer recIndex, String recipientInternalId, RaddInfo raddInfo, Instant eventTimestamp) {
        PnAuditLogEvent logEvent = generateRaddRetrievedAuditLog(iun, recipientInternalId);
        logEvent.log();

        return Mono.fromCallable(() -> timelineUtils.checkIsNotificationCancellationRequested(iun))
                .flatMap(isNotificationCancelled -> {
                    if (Boolean.TRUE.equals(isNotificationCancelled)) {
                        logEvent.generateWarning("For this notification a cancellation has been requested - iun={} id={}", iun, recIndex).log();
                        return Mono.empty();
                    }

                    log.debug("Notification is not already cancelled - iun={} internalId={} recIdx={}", iun, recipientInternalId, recIndex);
                    return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                            .flatMap(notificationInt -> Mono.fromCallable(() -> {
                                paperNotificationFailedService.deleteNotificationFailed(String.valueOf(recIndex), iun);
                                return notificationInt;
                            }))
                            .flatMap(notificationInt -> Mono.fromCallable(() -> {
                                TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationRaddRetrieveTimelineElement(notificationInt, recIndex, raddInfo, eventTimestamp);
                                return timelineService.addTimelineElement(timelineElementInternal, notificationInt);
                            }))
                            .doOnNext(timelineElementAdded -> {
                                if(Boolean.FALSE.equals(timelineElementAdded)) {
                                    logEvent.generateWarning("Timeline element wasn't added to notification").log();
                                }
                            })
                            .doOnSuccess(success -> logEvent.generateSuccess().log())
                            .doOnError(err -> logEvent.generateFailure("Exception in View retrieve Radd notification iun={} internalId={}", iun, recipientInternalId, err).log())
                            .then();
                });
    }

    private PnAuditLogEvent generateRaddRetrievedAuditLog(String iun, String recipientInternalId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder.before(PnAuditLogEventType.AUD_NT_RADD_OPEN,
                "View radd retrieve notification - iun={} recipientInternalId={}", iun, recipientInternalId
                )
                .iun(iun)
                .build();
    }

}
