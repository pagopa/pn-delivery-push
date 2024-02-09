package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationViewLegalFactCreationResponseHandler {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final NotificationCost notificationCost;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final NotificationUtils notificationUtils;
    private final TimelineUtils timelineUtils;
    
    public void handleLegalFactCreationResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleLegalFactCreationResponse process - iun={} legalFactId={}", iun, actionDetails.getKey());
        PnAuditLogEvent recipientAccessLegalFactAuditLog = getAuditLogLegalFact(iun, recIndex, actionDetails);
        recipientAccessLegalFactAuditLog.log();
        
        try {
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            Optional<NotificationViewedCreationRequestDetailsInt> notificationViewLegalFactCreationDetailsOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), NotificationViewedCreationRequestDetailsInt.class);

            if(notificationViewLegalFactCreationDetailsOpt.isPresent()){

                NotificationViewedCreationRequestDetailsInt timelineDetails = notificationViewLegalFactCreationDetailsOpt.get();

                RaddInfo raddInfo = getRaddInfo(timelineDetails);
                
                MDCUtils.addMDCToContextAndExecute(
                        notificationCost.getNotificationCostForViewed(notification, recIndex)
                                .doOnSuccess( cost -> log.info("Completed getNotificationCost cost={}- iun={} id={}", cost, notification.getIun(), recIndex))
                                .flatMap(responseCost -> {
                                    Integer cost = responseCost.orElse(null);
                                    return addTimelineAndDeletePaperNotificationFailed(notification, recIndex, raddInfo, timelineDetails.getEventTimestamp(),
                                            timelineDetails.getLegalFactId(), cost, timelineDetails.getDelegateInfo());
                                })
                ).block();
                
                recipientAccessLegalFactAuditLog.generateSuccess().log();

            } else {
                log.error("handleLegalFactCreationResponse failed, timelineId is not present {} - iun={} id={}", actionDetails.getTimelineId(), iun, recIndex);
                throw new PnInternalException("handleLegalFactCreationResponse timelineId is not present", ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
            }

        } catch (Exception ex){
            recipientAccessLegalFactAuditLog.generateFailure( "Saving legalFact FAILURE type={} fileKey={} iun={} recIndex={} ex={}", LegalFactCategoryInt.RECIPIENT_ACCESS, actionDetails.getKey(), iun, recIndex, ex).log();
            throw ex;
        }
    }

    private PnAuditLogEvent getAuditLogLegalFact(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "Saving legalFact type={} fileKey={} iun={} recIndex={}", LegalFactCategoryInt.RECIPIENT_ACCESS, actionDetails.getKey(), iun, recIndex)
                .iun(iun)
                .build();
    }

    private RaddInfo getRaddInfo(NotificationViewedCreationRequestDetailsInt timelineDetails) {
        RaddInfo raddInfo = null;
        if( timelineDetails.getRaddType() != null && timelineDetails.getRaddTransactionId() != null){
            raddInfo = RaddInfo.builder()
                    .type(timelineDetails.getRaddType())
                    .transactionId(timelineDetails.getRaddTransactionId())
                    .build();
        }
        return raddInfo;
    }

    @NotNull
    private Mono<Void> addTimelineAndDeletePaperNotificationFailed(NotificationInt notification, Integer recIndex, RaddInfo raddInfo, Instant eventTimestamp, String legalFactId, Integer cost, DelegateInfoInt delegateInfoInt) {

        return Mono.fromCallable( () -> {
                    log.info("addTimelineAndDeletePaperNotificationFailed - iun={} id={}" , notification.getIun(), recIndex);
                    return notificationUtils.getRecipientFromIndex(notification, recIndex);
                })
                .flatMap( recipient ->
                        //Viene eliminata l'eventuale istanza di notifica fallita dal momento che la stessa è stata letta
                        Mono.fromCallable( () -> timelineUtils.buildNotificationViewedTimelineElement(notification, recIndex, legalFactId, cost, raddInfo,
                                        delegateInfoInt, eventTimestamp))
                                .flatMap( timelineElementInternal ->
                                        Mono.fromRunnable( () -> addTimelineElement(timelineElementInternal, notification))
                                                .doOnSuccess( res -> log.info( "addTimelineElement OK {}", notification.getIun()))
                                                .map(res -> Mono.empty())
                                )
                                .thenEmpty(
                                        Mono.fromRunnable( () -> paperNotificationFailedService.deleteNotificationFailed(recipient.getInternalId(), notification.getIun()))
                                )
                );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
