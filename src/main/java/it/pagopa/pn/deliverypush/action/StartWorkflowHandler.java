package it.pagopa.pn.deliverypush.action;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
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
import java.time.Instant;
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
        try {
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            try {
                //Validazione degli allegati della notifica
                checkAttachmentUtils.validateAttachment(notification);

                String legalFactId = saveLegalFactsService.saveNotificationReceivedLegalFact(notification);

                addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId), notification);
                Instant requestAcceptedDate = Instant.now();

                //Start del workflow per ogni recipient della notifica
                for (NotificationRecipientInt recipient : notification.getRecipients()) {
                    Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
                    startNotificationWorkflowForRecipient(notification, recIndex, requestAcceptedDate);
                }
            } catch (PnValidationException ex) {
                handleValidationError(notification, ex);
            }
        }catch (PnInternalException ex){
            log.error("exception starting workflow", ex);
            throw ex;
        } catch (Exception ex){
            log.error("exception starting workflow", ex);
            throw new PnInternalException("Cannot start workflow", ex);
        }
    }

    private void startNotificationWorkflowForRecipient(NotificationInt notification, Integer recIndex, Instant requestAcceptedDate) {
        log.info("Start notification workflow - iun {} id {}", notification.getIun(), recIndex);
        // ... genero il pdf dell'AAR, salvo su Safestorage e genero elemento in timeline AAR_GENERATION, potrebbe servirmi dopo ...
        aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(notification, recIndex);
        //... Invio messaggio di cortxesia ...
        courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, recIndex, requestAcceptedDate);
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
