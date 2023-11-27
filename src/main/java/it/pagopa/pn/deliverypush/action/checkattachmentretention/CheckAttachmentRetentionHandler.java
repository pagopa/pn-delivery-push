package it.pagopa.pn.deliverypush.action.checkattachmentretention;

import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@AllArgsConstructor
@Slf4j
public class CheckAttachmentRetentionHandler {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushConfigs configs;
    private final SchedulerService schedulerService;
    
    public void handleCheckAttachmentRetentionBeforeExpiration(String iun){
        log.debug("Start handleCheckAttachmentRetentionBeforeExpiration - iun={}", iun);
        boolean isNotificationRefined = timelineService.getTimeline(iun, false)
                .stream().anyMatch(elem -> TimelineElementCategoryInt.REFINEMENT.equals(elem.getCategory()) || TimelineElementCategoryInt.NOTIFICATION_VIEWED.equals(elem.getCategory()));
        
        if(! isNotificationRefined){
            log.info("Notification isn't refined, need to update retention - iun={} ", iun);
            NotificationInt notification = notificationService.getNotificationByIun(iun);
            
            //Viene aggiornata la retention degli attachment e inserita una nuova action che, nuovamente, agisca in caso di retention in scadenza
            scheduleCheckAttachmentRetentionBeforeExpiration(iun);
            attachmentUtils.changeAttachmentsRetention(notification, configs.getAttachmentDaysToAddAfterExpiration()).collectList().block();
        }else{
            log.info("Notification is already refined, don't need to update retention - iun={} ", iun);
        }
    }

    private void scheduleCheckAttachmentRetentionBeforeExpiration(String iun) {
        log.debug("Start scheduleCheckAttachmentRetentionBeforeExpiration - attachmentDaysToAddAfterExpiration={} checkAttachmentDaysBeforeExpiration={} iun={}",
                configs.getAttachmentDaysToAddAfterExpiration(), configs.getCheckAttachmentDaysBeforeExpiration(), iun);
        int checkAttachmentDaysToWait = configs.getAttachmentDaysToAddAfterExpiration() - configs.getCheckAttachmentDaysBeforeExpiration();
        Instant checkAttachmentDate = Instant.now().plus(checkAttachmentDaysToWait, ChronoUnit.DAYS);

        log.info("Scheduling checkAttachmentRetention schedulingDate={} - iun={}", checkAttachmentDate, iun);
        schedulerService.scheduleEvent(iun, checkAttachmentDate, ActionType.CHECK_ATTACHMENT_RETENTION);
    }

}
