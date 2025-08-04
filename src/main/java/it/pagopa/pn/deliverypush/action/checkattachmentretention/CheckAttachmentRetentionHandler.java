package it.pagopa.pn.deliverypush.action.checkattachmentretention;

import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogFailureWorkflowTimeoutDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class CheckAttachmentRetentionHandler {
    private final NotificationService notificationService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushConfigs configs;
    private final SchedulerService schedulerService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;

    public void handleCheckAttachmentRetentionBeforeExpiration(String iun, Instant lastActionNotBefore){
        log.debug("Start handleCheckAttachmentRetentionBeforeExpiration - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        boolean notificationHasTriggeredAttachmentRetentionUpdate = notification.getRecipients().stream()
                .allMatch(recipient -> {
                    int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
                    return timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, recIndex);
                });
        
        if(! notificationHasTriggeredAttachmentRetentionUpdate ){
            log.info("Notification isn't refined, need to update retention - iun={} ", iun);
            
            //Viene aggiornata la retention degli attachment e inserita una nuova action che, nuovamente, agisca in caso di retention in scadenza
            int attachmentTimeToAddAfterExpiration = (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
            scheduleNewCheckAndUpdateAttachmentRetention(iun, lastActionNotBefore, notification, attachmentTimeToAddAfterExpiration);
        }else{
            //Metodo handleAnalogDeliveryTimeoutIfPresent da rimuovere nei prossimi sviluppi.
            handleAnalogDeliveryTimeoutIfPresent(iun, notification);
        }
    }

    private void scheduleNewCheckAndUpdateAttachmentRetention(String iun, Instant lastActionNotBefore, NotificationInt notification, int attachmentTimeToAddAfterExpiration) {
        scheduleCheckAttachmentRetentionBeforeExpiration(iun, lastActionNotBefore);
        attachmentUtils.changeAttachmentsRetention(notification, attachmentTimeToAddAfterExpiration).blockLast();
    }


    private void handleAnalogDeliveryTimeoutIfPresent(String iun, NotificationInt notification) {
        Optional<AnalogFailureWorkflowTimeoutDetailsInt> analogFailureWorkflowTimeoutDetailsOpt = checkAndGetLastTimeoutDateFromFailureTimeout(iun);
        if (analogFailureWorkflowTimeoutDetailsOpt.isEmpty()) {
            log.info("Notification is already refined, don't need to update retention - iun={} ", iun);
            return;
        }

        AnalogFailureWorkflowTimeoutDetailsInt analogFailureWorkflowTimeoutDetailsWithLastTimeoutDate = analogFailureWorkflowTimeoutDetailsOpt.get();
        int retentionAttachmentDaysAfterDeliveryTimeout = configs.getRetentionAttachmentDaysAfterDeliveryTimeout();
        /* Se la configurazione è impostata a zero, la retention dei documenti dovrà essere costantemente aggiornata e la action dovrà essere rischedulata. */
        if (retentionAttachmentDaysAfterDeliveryTimeout == 0) {
            log.info("RetentionAttachmentDaysAfterDeliveryTimeout is 0, scheduling checkAttachmentRetention with attachmentTimeToAddAfterExpiration for iun={}", iun);
            int attachmentTimeToAddAfterExpiration = (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
            scheduleNewCheckAndUpdateAttachmentRetention(iun, Instant.now(), notification, attachmentTimeToAddAfterExpiration);
            /* Se la configurazione è maggiore di zero, saranno conteggiati i giorni dalla data di Timeout. Se il risultato è un numero positivo,
             * verrà rischedulata l'action per l'aggiornamento della retention degli attachment. Se il risultato è 0 o un numero negativo, non sarà necessario aggiornare la retention
             * in quanto il timeout è già scaduto e non sono previsti aggiornamenti della retention per i documenti allegati. */
        } else if (retentionAttachmentDaysAfterDeliveryTimeout > 0) {
            log.info("RetentionAttachmentDaysAfterDeliveryTimeout is {}, checking if it's necessary to reschedule the action and update the retention for iun={}", retentionAttachmentDaysAfterDeliveryTimeout, iun);
            Instant timeoutDate = analogFailureWorkflowTimeoutDetailsWithLastTimeoutDate.getTimeoutDate();
            int daysToAdd = getDaysToAddFromTimeout(timeoutDate, retentionAttachmentDaysAfterDeliveryTimeout);
            if (daysToAdd > 0) {
                log.info("Scheduling and updating checkAttachmentRetention with daysToAdd={} for iun={}", daysToAdd, iun);
                scheduleNewCheckAndUpdateAttachmentRetention(iun, Instant.now(), notification, daysToAdd);
            } else {
                log.info("The timeout element has already used up the expected retention, don't need to update retention - iun={} ", iun);
            }
        }
    }

    private int getDaysToAddFromTimeout(Instant timeoutDate, Integer retentionAttachmentDaysAfterDeliveryTimeout) {
        Duration timeFromTimeout = Duration.between(timeoutDate, Instant.now());
        int daysFromTimeout = ((int) timeFromTimeout.toDays());
        return retentionAttachmentDaysAfterDeliveryTimeout - daysFromTimeout;
    }

    private Optional<AnalogFailureWorkflowTimeoutDetailsInt> checkAndGetLastTimeoutDateFromFailureTimeout(String iun) {
        return timelineService.getTimeline(iun, false)
                .stream()
                .filter(timeline -> timeline.getCategory() == TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_TIMEOUT)
                .map(timeline -> (AnalogFailureWorkflowTimeoutDetailsInt) timeline.getDetails())
                .max(Comparator.comparing(AnalogFailureWorkflowTimeoutDetailsInt::getTimeoutDate));
    }

    private void scheduleCheckAttachmentRetentionBeforeExpiration(String iun, Instant lastActionNotBefore) {
        Duration attachmentTimeToAddAfterExpiration = configs.getTimeParams().getAttachmentTimeToAddAfterExpiration();
        Duration checkAttachmentTimeBeforeExpiration = configs.getTimeParams().getCheckAttachmentTimeBeforeExpiration();
        log.debug("Start scheduleCheckAttachmentRetentionBeforeExpiration - attachmentDaysToAddAfterExpiration={} checkAttachmentDaysBeforeExpiration={} iun={}",
                attachmentTimeToAddAfterExpiration, checkAttachmentTimeBeforeExpiration, iun);
        
        Duration checkAttachmentTimeToWait = attachmentTimeToAddAfterExpiration.minus(checkAttachmentTimeBeforeExpiration);
        Instant checkAttachmentDate = lastActionNotBefore.plus(checkAttachmentTimeToWait);

        log.info("Scheduling checkAttachmentRetention schedulingDate={} - iun={}", checkAttachmentDate, iun);
        schedulerService.scheduleEvent(iun, checkAttachmentDate, ActionType.CHECK_ATTACHMENT_RETENTION);
    }

}
