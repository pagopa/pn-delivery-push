package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
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
    
    public Mono<Void> startVewNotificationProcess(NotificationInt notification,
                                            NotificationRecipientInt recipient,
                                            Integer recIndex,
                                            RaddInfo raddInfo,
                                            DelegateInfoInt delegateInfo,
                                            Instant eventTimestamp
    ) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), recIndex);
        return Mono.fromRunnable( () ->{
                    attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement());
                    Mono<BaseRecipientDtoInt> monoRecipientDenomination = Mono.empty();
                    if(delegateInfo != null){
                        monoRecipientDenomination = confidentialInformationService.getRecipientDenominationByInternalId(delegateInfo.getInternalId());
                    }
                    Mono<String> monoLegalFactId = legalFactStore.saveNotificationViewedLegalFact(notification, recipient, instantNowSupplier.get());
                    Mono<Integer> monoNotificationCost = notificationCost.getNotificationCost(notification, recIndex);

                    Mono.zip(monoLegalFactId, monoNotificationCost, monoRecipientDenomination)
                            .doOnSuccess( res -> {
                                log.info("Get information for view notification process - iun={} id={}", notification.getIun(), recIndex);
                            })
                            .doOnNext(
                                    response -> {
                                        
                                        if(response.getT3() != null){
                                            String denomination = response.getT3().getDenomination();
                                            delegateInfo.toBuilder()
                                                    .denomination(denomination).build();

                                        }
                                        Integer cost = response.getT2();
                                        String legalFactId = response.getT1();
                                        
                                        addTimelineElement(
                                                timelineUtils.buildNotificationViewedTimelineElement(notification, recIndex, legalFactId, cost, raddInfo,
                                                        delegateInfo, eventTimestamp),
                                                notification
                                        );
                                    }
                            ).doOnNext( res ->
                                    paperNotificationFailedService.deleteNotificationFailed(recipient.getInternalId(), notification.getIun()) //Viene eliminata l'eventuale istanza di notifica fallita dal momento che la stessa è stata letta
                            );
                }
        );
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
