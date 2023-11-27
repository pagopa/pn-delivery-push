package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@AllArgsConstructor
@Slf4j
public class ReceivedLegalFactCreationRequest {
    private final SaveLegalFactsService saveLegalFactsService;
    private final DocumentCreationRequestService documentCreationRequestService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final AttachmentUtils attachmentUtils;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs configs;
    
    
    public void saveNotificationReceivedLegalFacts(String iun) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        // cambio lo stato degli attachment in ATTACHED e schedulo la verifica retention degli attachment prima che la stessa scada
        scheduleCheckAttachmentRetentionBeforeExpiration(iun);
        attachmentUtils.changeAttachmentsStatusToAttached(notification);

        // Invio richiesta di creazione di atto opponibile a terzi di avvenuta ricezione da parte di PN a SafeStorage
        String legalFactId = saveLegalFactsService.sendCreationRequestForNotificationReceivedLegalFact(notification);

        TimelineElementInternal timelineElementInternal = timelineUtils.buildSenderAckLegalFactCreationRequest(notification, legalFactId);
        addTimelineElement( timelineElementInternal , notification);
        
        //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
        documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.SENDER_ACK, timelineElementInternal.getElementId());
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    private void scheduleCheckAttachmentRetentionBeforeExpiration(String iun) {
        log.debug("Start scheduleCheckAttachmentRetentionBeforeExpiration - attachmentRetentionDaysAfterValidation={} checkAttachmentDaysBeforeExpiration={} iun={}",
                configs.getAttachmentRetentionDaysAfterValidation(), configs.getCheckAttachmentDaysBeforeExpiration(), iun);
        int checkAttachmentDaysToWait = configs.getAttachmentRetentionDaysAfterValidation() - configs.getCheckAttachmentDaysBeforeExpiration();
        Instant checkAttachmentDate = Instant.now().plus(checkAttachmentDaysToWait, ChronoUnit.DAYS);
        
        log.debug("Scheduling checkAttachmentRetention schedulingDate={} - iun={}", checkAttachmentDate, iun);
        schedulerService.scheduleEvent(iun, checkAttachmentDate, ActionType.CHECK_ATTACHMENT_RETENTION);
    }
}
