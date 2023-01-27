package it.pagopa.pn.deliverypush.action.startworkflow;


import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class StartWorkflowHandler {
    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final AttachmentUtils attachmentUtils;
    private final DocumentCreationRequestService documentCreationRequestService;
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        try {
            //Validazione degli allegati della notifica
            attachmentUtils.validateAttachment(notification);

            saveNotificationReceivedLegalFacts(notification);

        } catch (PnValidationException ex) {
            handleValidationError(notification, ex);
        }
    }
    
    private void saveNotificationReceivedLegalFacts(NotificationInt notification) {
        // salvo il legalfactid di avvenuta ricezione da parte di PN
        String legalFactId = saveLegalFactsService.sendCreationRequestForNotificationReceivedLegalFact(notification);
        
        TimelineElementInternal timelineElementInternal = timelineUtils.buildSenderAckLegalFactCreationRequest(notification, legalFactId);
        addTimelineElement( timelineElementInternal , notification);
        
        //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
        documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.SENDER_ACK, timelineElementInternal.getElementId());
    }
    
    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors = new ArrayList<>();
        if (Objects.nonNull( ex.getProblem() )) {
            errors = Collections.singletonList( ex.getProblem().getDetail() );
        }
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
