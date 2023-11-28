package it.pagopa.pn.deliverypush.action.checkattachmentretention;

import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class CheckAttachmentRetentionHandler {
    private final NotificationService notificationService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushConfigs configs;
    private final SchedulerService schedulerService;
    private final TimelineUtils timelineUtils;

    public void handleCheckAttachmentRetentionBeforeExpiration(String iun){
        log.debug("Start handleCheckAttachmentRetentionBeforeExpiration - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        boolean isNotificationViewedRefinedOrCancelled = notification.getRecipients().stream()
                .allMatch(recipient -> {
                    int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
                    return timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, recIndex);
                });
        
        if(! isNotificationViewedRefinedOrCancelled ){
            log.info("Notification isn't refined, need to update retention - iun={} ", iun);
            
            //Viene aggiornata la retention degli attachment e inserita una nuova action che, nuovamente, agisca in caso di retention in scadenza
            scheduleCheckAttachmentRetentionBeforeExpiration(iun);
            
            int attachmentTimeToAddAfterExpiration = (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
            attachmentUtils.changeAttachmentsRetention(notification, attachmentTimeToAddAfterExpiration).blockLast();
        }else{
            log.info("Notification is already refined, don't need to update retention - iun={} ", iun);
        }
    }

    private void scheduleCheckAttachmentRetentionBeforeExpiration(String iun) {
        Duration attachmentTimeToAddAfterExpiration = configs.getTimeParams().getAttachmentTimeToAddAfterExpiration();
        Duration checkAttachmentTimeBeforeExpiration = configs.getTimeParams().getCheckAttachmentTimeBeforeExpiration();
        log.debug("Start scheduleCheckAttachmentRetentionBeforeExpiration - attachmentDaysToAddAfterExpiration={} checkAttachmentDaysBeforeExpiration={} iun={}",
                attachmentTimeToAddAfterExpiration, checkAttachmentTimeBeforeExpiration, iun);
        
        Duration checkAttachmentTimeToWait = attachmentTimeToAddAfterExpiration.minus(checkAttachmentTimeBeforeExpiration);
        Instant checkAttachmentDate = Instant.now().plus(checkAttachmentTimeToWait);

        log.info("Scheduling checkAttachmentRetention schedulingDate={} - iun={}", checkAttachmentDate, iun);
        schedulerService.scheduleEvent(iun, checkAttachmentDate, ActionType.CHECK_ATTACHMENT_RETENTION);
    }

}
