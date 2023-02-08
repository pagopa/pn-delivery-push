package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
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
    private final DocumentCreationRequestService documentCreationRequestService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public Mono<Void> startVewNotificationProcess(NotificationInt notification,
                                            NotificationRecipientInt recipient,
                                            Integer recIndex,
                                            RaddInfo raddInfo,
                                            DelegateInfoInt delegateInfo,
                                            Instant eventTimestamp) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), recIndex);
        return attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).collectList()
            .then(
                legalFactStore.sendCreationRequestForNotificationViewedLegalFact(notification, recipient, delegateInfo, instantNowSupplier.get())
                        .doOnSuccess( legalFactId -> log.info("Completed sendCreationRequestForNotificationViewedLegalFact legalFactId={} - iun={} id={}", legalFactId, notification.getIun(), recIndex))
                        .flatMap(legalFactId ->
                                Mono.fromRunnable( () -> {
                                    TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(notification, recIndex, legalFactId, raddInfo, delegateInfo, eventTimestamp);
                                    addTimelineElement( timelineElementInternal , notification);
        
                                    //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
                                    documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.RECIPIENT_ACCESS, timelineElementInternal.getElementId());
                                })
                        )
            );
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
