package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
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
    private final SaveLegalFactsService legalFactStore;
    private final DocumentCreationRequestService documentCreationRequestService;
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
                                            Instant eventTimestamp) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), recIndex);
        if(delegateInfo != null){
            return attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).collectList()
                    .then(getDelegateInfoAndHandleLegalFactCreation(notification, recipient, recIndex, raddInfo, delegateInfo, eventTimestamp));
        } else {
            return attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).collectList()
                    .then(handleLegalFactCreation(notification, recipient, recIndex, raddInfo, eventTimestamp, null));
        }
    }

    private Mono<Void> getDelegateInfoAndHandleLegalFactCreation(NotificationInt notification, NotificationRecipientInt recipient, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo, Instant eventTimestamp) {
        return confidentialInformationService.getRecipientInformationByInternalId(delegateInfo.getInternalId())
                .doOnSuccess( baseRecipientDto -> log.info("Completed getBaseRecipientDtoIntMono - iun={} id={} taxId={}" , notification.getIun(), recIndex, LogUtils.maskTaxId(baseRecipientDto.getTaxId())))
                .map(baseRecipientDto ->
                        delegateInfo.toBuilder()
                                .denomination(baseRecipientDto.getDenomination())
                                .taxId(baseRecipientDto.getTaxId())
                                .build()
                ).flatMap(delegateInfoEnriched ->
                        handleLegalFactCreation(notification, recipient, recIndex, raddInfo, eventTimestamp, delegateInfoEnriched)
                );
    }

    @NotNull
    private Mono<Void> handleLegalFactCreation(NotificationInt notification, NotificationRecipientInt recipient, Integer recIndex, RaddInfo raddInfo, Instant eventTimestamp, DelegateInfoInt delegateInfo) {
        return legalFactStore.sendCreationRequestForNotificationViewedLegalFact(notification, recipient, delegateInfo, eventTimestamp)
                .doOnSuccess( legalFactId -> log.info("Completed sendCreationRequestForNotificationViewedLegalFact legalFactId={} - iun={} id={}", legalFactId, notification.getIun(), recIndex))
                .flatMap(legalFactId ->
                        Mono.fromRunnable( () -> {
                            TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(notification, recIndex, legalFactId, raddInfo, delegateInfo, eventTimestamp);
                            addTimelineElement( timelineElementInternal , notification);

                            //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
                            documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.RECIPIENT_ACCESS, timelineElementInternal.getElementId());
                        })
                );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
