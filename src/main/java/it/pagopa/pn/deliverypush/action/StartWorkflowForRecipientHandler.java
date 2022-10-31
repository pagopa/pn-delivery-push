package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
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
    private final PnAuditLogBuilder auditLogBuilder;
    
    public StartWorkflowForRecipientHandler(CourtesyMessageUtils courtesyMessageUtils,
                                            SchedulerService schedulerService,
                                            AarUtils aarUtils,
                                            NotificationService notificationService,
                                            PnAuditLogBuilder pnAuditLogBuilder) {
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.schedulerService = schedulerService;
        this.aarUtils = aarUtils;
        this.notificationService = notificationService;
        this.auditLogBuilder = pnAuditLogBuilder;
    }

    public void startNotificationWorkflowForRecipient(String iun, int recIndex) {
        log.info("Start notification workflow for recipient - iun {} id {}", iun, recIndex);
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        generateAAR(notification, recIndex);

        //... Invio messaggio di cortesia ... 
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, recIndex);

        //... e viene schedulato il processo di scelta della tipologia di notificazione
        scheduleChooseDeliveryMode(iun, recIndex);
    }

    private void generateAAR(NotificationInt notification, Integer recIndex) {
        // ... genero il pdf dell'AAR, salvo su Safestorage e genero elemento in timeline AAR_GENERATION, potrebbe servirmi dopo ...
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
    }

    private void scheduleChooseDeliveryMode(String iun, Integer recIndex) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling choose delivery mode schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.CHOOSE_DELIVERY_MODE);
    }
    
}
