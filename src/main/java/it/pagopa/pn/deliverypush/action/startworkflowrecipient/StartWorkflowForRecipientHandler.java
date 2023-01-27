package it.pagopa.pn.deliverypush.action.startworkflowrecipient;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class StartWorkflowForRecipientHandler {
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final SchedulerService schedulerService;
    private final AarUtils aarUtils;
    private final NotificationService notificationService;
    
    public StartWorkflowForRecipientHandler(CourtesyMessageUtils courtesyMessageUtils,
                                            SchedulerService schedulerService,
                                            AarUtils aarUtils,
                                            NotificationService notificationService) {
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.schedulerService = schedulerService;
        this.aarUtils = aarUtils;
        this.notificationService = notificationService;
    }

    public void startNotificationWorkflowForRecipient(String iun, int recIndex, RecipientsWorkflowDetails details) {
        log.info("Start notification workflow for recipient - iun {} id {} token {}", iun, recIndex, details.getQuickAccessLinkToken());
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        generateAAR(notification, recIndex, details.getQuickAccessLinkToken());

        //... Invio messaggio di cortesia ... 
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, recIndex);

        //... e viene schedulato il processo di scelta della tipologia di notificazione
        scheduleChooseDeliveryMode(iun, recIndex);
    }
    private void generateAAR(NotificationInt notification, Integer recIndex, String quickAccessToken) {
        // ... genero il pdf dell'AAR, salvo su Safestorage e genero elemento in timeline AAR_GENERATION, potrebbe servirmi dopo ...
        
        //TODO AUDITLOG DA ELIMINARE
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_AAR, "Notification AAR generation for iun={} and recIndex={}", notification.getIun(), recIndex)
                .iun(notification.getIun())
                .build();
        logEvent.log();
        try {
            aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineevent(notification, recIndex, quickAccessToken);
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure("Exception on generation of AAR", exc.getMessage()).log();
            throw exc;
        }
    }

    private void scheduleChooseDeliveryMode(String iun, Integer recIndex) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling choose delivery mode schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.CHOOSE_DELIVERY_MODE);
    }
    
}
 