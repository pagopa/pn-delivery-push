package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewNotification {

    private final InstantNowSupplier instantNowSupplier;
    private final SaveLegalFactsService legalFactStore;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final NotificationCost notificationCost;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final ConfidentialInformationService confidentialInformationService;
    private final NotificationUtils notificationUtils;

    public Mono<Void> startVewNotificationProcess(NotificationInt notification,
                                            NotificationRecipientInt recipient,
                                            Integer recIndex,
                                            RaddInfo raddInfo,
                                            DelegateInfoInt delegateInfo,
                                            Instant eventTimestamp
    ) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), recIndex);
        return Mono.fromRunnable( () -> attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()))
            .then(
                legalFactStore.saveNotificationViewedLegalFact(notification, recipient, instantNowSupplier.get())
                        .doOnSuccess( legalFactId -> log.info("Completed saveNotificationViewedLegalFact legalFactId={} - iun={} id={}", legalFactId, notification.getIun(), recIndex))
                        .flatMap(legalFactId ->
                                notificationCost.getNotificationCost(notification, recIndex)
                                        .doOnSuccess( cost -> log.info("Completed getNotificationCost cost={}- iun={} id={}", cost, notification.getIun(), recIndex))
                                        .flatMap(responseCost -> {
                                            Integer cost = responseCost.orElse(null);
                                            return getDenominationAndSaveInTimeline(notification, recIndex, raddInfo, eventTimestamp, legalFactId, cost, delegateInfo);
                                        })
                        )
            );
    }
    
    private Mono<Void> getDenominationAndSaveInTimeline(
            NotificationInt notification,
            Integer recIndex,
            RaddInfo raddInfo,
            Instant eventTimestamp,
            String legalFactId,
            Integer cost,
            DelegateInfoInt delegateInfo){
        
        if ( delegateInfo != null){
            log.debug("View is from delegate - iun={} id={}" , notification.getIun(), recIndex);

            return confidentialInformationService.getRecipientInformationByInternalId(delegateInfo.getInternalId())
                    .doOnSuccess( baseRecipientDto -> log.info("Completed getBaseRecipientDtoIntMono - iun={} id={}" , notification.getIun(), recIndex))
                    .map(baseRecipientDto ->
                            delegateInfo.toBuilder()
                                    .denomination(baseRecipientDto.getDenomination())
                                    .taxId(baseRecipientDto.getTaxId())
                                    .build()
                    )
                    .flatMap(delegateInfoInt ->{
                        log.info("Completed flatMap - iun={} id={}" , notification.getIun(), recIndex);
                        return addTimelineAndDeletePaperNotificationFailed(notification, recIndex, raddInfo, eventTimestamp, legalFactId, cost, delegateInfoInt);
                    });
        } else {
            log.debug("View is not from delegate - iun={} id={}" , notification.getIun(), recIndex);
            return addTimelineAndDeletePaperNotificationFailed(notification, recIndex, raddInfo, eventTimestamp, legalFactId, cost, null);
        }
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
