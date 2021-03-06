package it.pagopa.pn.deliverypush.action;


import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StartWorkflowHandler {
    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationService notificationService;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final ChooseDeliveryModeHandler chooseModeHandler;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final CheckAttachmentUtils checkAttachmentUtils;
    private final NotificationUtils notificationUtils;
    private final AarUtils aarUtils;

    public StartWorkflowHandler(
            SaveLegalFactsService saveLegalFactsService,
            NotificationService notificationService,
            CourtesyMessageUtils courtesyMessageUtils,
            ChooseDeliveryModeHandler chooseDeliveryType,
            TimelineService timelineService,
            TimelineUtils timelineUtils,
            CheckAttachmentUtils checkAttachmentUtils,
            NotificationUtils notificationUtils,
            AarUtils aarUtils
    ) {
        this.saveLegalFactsService = saveLegalFactsService;
        this.notificationService = notificationService;
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.chooseModeHandler = chooseDeliveryType;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.checkAttachmentUtils = checkAttachmentUtils;
        this.notificationUtils = notificationUtils;
        this.aarUtils = aarUtils;
    }
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        try {
            //Validazione degli allegati della notifica
            checkAttachmentUtils.validateAttachment(notification);

            saveNotificationReceivedLegalFacts(notification);

            //Start del workflow per ogni recipient della notifica
            for (NotificationRecipientInt recipient : notification.getRecipients()) {
                Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
                startNotificationWorkflowForRecipient(notification, recIndex);
            }
        } catch (PnValidationException ex) {
            handleValidationError(notification, ex);
        }
    }

    private void saveNotificationReceivedLegalFacts(NotificationInt notification) {
            String legalFactId = saveLegalFactsService.saveNotificationReceivedLegalFact(notification);
            addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId), notification);
    }

    private void startNotificationWorkflowForRecipient(NotificationInt notification, Integer recIndex) {
        log.info("Start notification workflow - iun {} id {}", notification.getIun(), recIndex);
        // ... genero il pdf dell'AAR, salvo su Safestorage e genero elemento in timeline AAR_GENERATION, potrebbe servirmi dopo ...
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_ARR, "Notification AAR generation for iun={} and recIndex={}", notification.getIun(), recIndex)
                .iun(notification.getIun())
                .build();
        logEvent.log();
        try {
            aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(notification, recIndex);
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure("Exception on generation of ARR", exc.getMessage()).log();
            throw exc;
        }
        //... Invio messaggio di cortxesia ...
        courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, recIndex);
        //... e inizializzato il processo di scelta della tipologia di notificazione
        chooseModeHandler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);
    }

    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors =  ex.getValidationErrors().stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
